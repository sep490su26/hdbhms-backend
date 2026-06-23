package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetRoomImagesByRoomIdUseCase;
import com.sep490.hdbhms.occupancy.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.occupancy.application.port.out.RoomRepository;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetRoomImagesByRoomIdService implements GetRoomImagesByRoomIdUseCase {
    RoomRepository roomRepository;
    RoomImageRepository roomImageRepository;
    ResourcePatternResolver resourcePatternResolver;

    private static final Map<String, String> ROOM_IMAGE_FALLBACK_SOURCES = createFallbackSources();

    private static Map<String, String> createFallbackSources() {
        Map<String, String> map = new HashMap<>();
        registerFallback(map, "P102", "P101", "P102", "P201", "P202", "P301", "P302", "P401", "P402");
        registerFallback(map, "P404", "P103", "P104", "P105", "P106", "P203", "P204", "P205", "P206", "P207", "P303", "P304", "P305", "P306", "P307", "P403", "P404", "P405", "P406", "P407", "P503", "P504", "P505", "P506");
        registerFallback(map, "P208", "P208", "P308");
        registerFallback(map, "P408", "P408", "P507");
        registerFallback(map, "P501", "P501");
        registerFallback(map, "P502", "P502");
        return Map.copyOf(map);
    }

    private static void registerFallback(Map<String, String> map, String sourceRoomCode, String... roomCodes) {
        String normalizedSource = normalizeRoomCode(sourceRoomCode);
        for (String roomCode : roomCodes) {
            map.put(normalizeRoomCode(roomCode), normalizedSource);
        }
    }

    private static String normalizeRoomCode(String roomCode) {
        String normalized = roomCode == null ? "" : roomCode.trim().toUpperCase();
        if (normalized.isEmpty()) {
            return normalized;
        }
        return normalized.startsWith("P") ? normalized : "P" + normalized;
    }

    private List<RoomImage> decorateImages(List<RoomImage> images, boolean fallback, String sourceRoomCode) {
        return images.stream()
                .map(image -> image.toBuilder()
                        .fallback(fallback)
                        .sourceRoomCode(sourceRoomCode)
                        .build())
                .toList();
    }

    private static boolean isSupportedImageFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".avif");
    }

    private static int naturalCompare(String left, String right) {
        String a = left == null ? "" : left;
        String b = right == null ? "" : right;
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int startI = i;
                int startJ = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                long numberA = Long.parseLong(a.substring(startI, i));
                long numberB = Long.parseLong(b.substring(startJ, j));
                if (numberA != numberB) {
                    return Long.compare(numberA, numberB);
                }
                continue;
            }
            int compared = Character.compare(Character.toLowerCase(ca), Character.toLowerCase(cb));
            if (compared != 0) {
                return compared;
            }
            i++;
            j++;
        }
        return Integer.compare(a.length(), b.length());
    }

    private List<String> listStaticSampleFiles(String sampleRoomCode) {
        String normalizedSampleRoomCode = normalizeRoomCode(sampleRoomCode);
        try {
            Resource[] resources = resourcePatternResolver.getResources(
                    "classpath:/static/room-samples/" + normalizedSampleRoomCode + "/*"
            );
            List<String> fileNames = new ArrayList<>();
            for (Resource resource : resources) {
                String fileName = resource.getFilename();
                if (isSupportedImageFile(fileName)) {
                    fileNames.add(fileName);
                }
            }
            fileNames.sort(Comparator.nullsLast(GetRoomImagesByRoomIdService::naturalCompare));
            return fileNames;
        } catch (IOException ex) {
            return List.of();
        }
    }

    private List<RoomImage> buildStaticFallbackImages(Long roomId, String sampleRoomCode) {
        List<String> sampleFiles = listStaticSampleFiles(sampleRoomCode);
        if (sampleFiles == null || sampleFiles.isEmpty()) {
            return List.of();
        }

        String normalizedSampleRoomCode = normalizeRoomCode(sampleRoomCode);
        List<RoomImage> images = new ArrayList<>();
        for (int index = 0; index < sampleFiles.size(); index++) {
            String fileName = sampleFiles.get(index);
            images.add(RoomImage.builder()
                    .roomId(roomId)
                    .sortOrder(index)
                    .fallbackUrl("/room-samples/" + normalizedSampleRoomCode + "/" + fileName)
                    .build());
        }
        return images;
    }

    private String resolveFallbackRoomCode(String roomCode) {
        return ROOM_IMAGE_FALLBACK_SOURCES.get(normalizeRoomCode(roomCode));
    }

    @Override
    public List<RoomImage> execute(GetRoomImagesByRoomIdQuery query) {
        Room room = roomRepository.findById(query.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));

        List<RoomImage> ownImages = roomImageRepository.findAllByRoomId(room.getId());
        if (!ownImages.isEmpty()) {
            return decorateImages(ownImages, false, null);
        }

        String fallbackRoomCode = resolveFallbackRoomCode(room.getRoomCode());
        if (fallbackRoomCode == null) {
            return ownImages;
        }

        List<RoomImage> staticFallbackImages = buildStaticFallbackImages(room.getId(), fallbackRoomCode);
        if (!staticFallbackImages.isEmpty()) {
            return decorateImages(staticFallbackImages, true, fallbackRoomCode);
        }

        Room fallbackRoom = roomRepository.findByRoomCode(fallbackRoomCode)
                .orElse(null);
        if (fallbackRoom == null || Objects.equals(fallbackRoom.getId(), room.getId())) {
            return ownImages;
        }

        List<RoomImage> fallbackImages = roomImageRepository.findAllByRoomId(fallbackRoom.getId());
        if (fallbackImages.isEmpty()) {
            return ownImages;
        }

        return decorateImages(fallbackImages, true, fallbackRoom.getRoomCode());
    }
}
