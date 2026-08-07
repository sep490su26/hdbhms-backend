package com.sep490.hdbhms.property.application.service;

import com.sep490.hdbhms.file.application.port.out.FileMetadataRepository;
import com.sep490.hdbhms.file.domain.model.FileMetadata;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.property.application.port.in.command.AttachRoomImageCommand;
import com.sep490.hdbhms.property.application.port.in.command.DeleteRoomImageCommand;
import com.sep490.hdbhms.property.application.port.in.usecase.AttachRoomImageUseCase;
import com.sep490.hdbhms.property.application.port.in.usecase.DeleteRoomImageUseCase;
import com.sep490.hdbhms.property.application.port.out.RoomImageRepository;
import com.sep490.hdbhms.property.application.port.out.RoomRepository;
import com.sep490.hdbhms.property.domain.model.RoomImage;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManageRoomImageService implements AttachRoomImageUseCase, DeleteRoomImageUseCase {
    RoomRepository roomRepository;
    RoomImageRepository roomImageRepository;
    FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional
    public RoomImage execute(AttachRoomImageCommand command) {
        roomRepository.findById(command.roomId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_NOT_FOUND));
        FileMetadata file = fileMetadataRepository.findById(command.fileId())
                .orElseThrow(() -> new AppException(ApiErrorCode.FILE_DOWNLOAD_FAILED));
        if (file.getCategory() != FileCategory.ROOM_IMAGE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File không phải ảnh phòng.");
        }

        return roomImageRepository.save(RoomImage.builder()
                .roomId(command.roomId())
                .fileId(command.fileId())
                .sortOrder(resolveSortOrder(command.roomId(), command.sortOrder()))
                .build());
    }

    @Override
    @Transactional
    public void execute(DeleteRoomImageCommand command) {
        RoomImage image = roomImageRepository.findById(command.imageId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_IMAGE_NOT_FOUND));
        if (!Objects.equals(image.getRoomId(), command.roomId())) {
            throw new AppException(ApiErrorCode.ROOM_IMAGE_NOT_FOUND);
        }
        roomImageRepository.deleteById(command.imageId());
    }

    private int resolveSortOrder(Long roomId, Integer requestedSortOrder) {
        if (requestedSortOrder != null && requestedSortOrder >= 0) {
            return requestedSortOrder;
        }
        return roomImageRepository.findAllByRoomId(roomId).stream()
                .map(RoomImage::getSortOrder)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(value -> value + 1)
                .orElse(0);
    }
}
