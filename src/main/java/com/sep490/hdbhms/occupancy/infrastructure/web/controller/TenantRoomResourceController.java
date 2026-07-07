package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.file.infrastructure.persistence.entity.FileMetadataEntity;
import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.valueObjects.AssetCondition;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
import com.sep490.hdbhms.occupancy.application.service.LeaseContractQueryService;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.RoomAssetRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.MeterReadingLatestResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomAssetResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants/{tenantId}/rooms/{roomId}")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantRoomResourceController {
    JpaRoomRepository roomRepository;
    JpaRoomAssetRepository roomAssetRepository;
    JpaMeterReadingRepository meterReadingRepository;
    JpaFileMetadataRepository fileMetadataRepository;
    LeaseContractQueryService leaseContractQueryService;

    @GetMapping("/assets")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<List<RoomAssetResponse>> getAssets(
            @PathVariable Long tenantId,
            @PathVariable Long roomId
    ) {
        ensureRoomExists(roomId);
        leaseContractQueryService.assertCurrentUserCanReadRoom(roomId);
        return ApiResponse.<List<RoomAssetResponse>>builder()
                .data(roomAssetRepository.findActiveByRoomId(roomId).stream()
                        .map(this::toAssetResponse)
                        .toList())
                .build();
    }

    @GetMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<RoomAssetResponse> getAsset(
            @PathVariable Long tenantId,
            @PathVariable Long roomId,
            @PathVariable Long assetId
    ) {
        leaseContractQueryService.assertCurrentUserCanReadRoom(roomId);
        return ApiResponse.<RoomAssetResponse>builder()
                .data(toAssetResponse(findRoomAsset(roomId, assetId)))
                .build();
    }

    @PostMapping("/assets")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<RoomAssetResponse> createAsset(
            @PathVariable Long tenantId,
            @PathVariable Long roomId,
            @RequestBody RoomAssetRequest request
    ) {
        RoomEntity room = ensureRoomExists(roomId);
        RoomAssetEntity asset = RoomAssetEntity.builder()
                .room(room)
                .assetName(requireAssetName(request))
                .assetCategory(request.assetCategory())
                .quantity(resolveQuantity(request.quantity()))
                .currentCondition(resolveCondition(request.currentCondition()))
                .description(request.description())
                .imageFile(resolveFile(request.fileImageId()))
                .build();
        return ApiResponse.<RoomAssetResponse>builder()
                .data(toAssetResponse(roomAssetRepository.save(asset)))
                .build();
    }

    @PutMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<RoomAssetResponse> updateAsset(
            @PathVariable Long tenantId,
            @PathVariable Long roomId,
            @PathVariable Long assetId,
            @RequestBody RoomAssetRequest request
    ) {
        RoomAssetEntity asset = findRoomAsset(roomId, assetId);
        asset.setAssetName(requireAssetName(request));
        asset.setAssetCategory(request.assetCategory());
        asset.setQuantity(resolveQuantity(request.quantity()));
        asset.setCurrentCondition(resolveCondition(request.currentCondition()));
        asset.setDescription(request.description());
        asset.setImageFile(resolveFile(request.fileImageId()));
        return ApiResponse.<RoomAssetResponse>builder()
                .data(toAssetResponse(roomAssetRepository.save(asset)))
                .build();
    }

    @DeleteMapping("/assets/{assetId}")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    public ApiResponse<Void> deleteAsset(
            @PathVariable Long tenantId,
            @PathVariable Long roomId,
            @PathVariable Long assetId
    ) {
        RoomAssetEntity asset = findRoomAsset(roomId, assetId);
        asset.setDeletedAt(LocalDateTime.now());
        roomAssetRepository.save(asset);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/meter-readings/latest")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','TENANT')")
    public ApiResponse<MeterReadingLatestResponse> getLatestMeterReadings(
            @PathVariable Long tenantId,
            @PathVariable Long roomId
    ) {
        ensureRoomExists(roomId);
        leaseContractQueryService.assertCurrentUserCanReadRoom(roomId);
        MeterReadingLatestResponse.Item electricity = null;
        MeterReadingLatestResponse.Item water = null;
        for (MeterReadingEntity reading : meterReadingRepository.findActiveByRoomIdLatestFirst(roomId)) {
            MeterType meterType = reading.getMeter().getMeterType();
            if (meterType == MeterType.ELECTRICITY && electricity == null) {
                electricity = toMeterReadingItem(reading);
            } else if (meterType == MeterType.WATER && water == null) {
                water = toMeterReadingItem(reading);
            }
            if (electricity != null && water != null) {
                break;
            }
        }
        return ApiResponse.<MeterReadingLatestResponse>builder()
                .data(new MeterReadingLatestResponse(electricity, water))
                .build();
    }

    private RoomEntity ensureRoomExists(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay phong."));
    }

    private RoomAssetEntity findRoomAsset(Long roomId, Long assetId) {
        return roomAssetRepository.findActiveByRoomIdAndId(roomId, assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Khong tim thay thiet bi phong."));
    }

    private RoomAssetResponse toAssetResponse(RoomAssetEntity asset) {
        FileMetadataEntity imageFile = asset.getImageFile();
        return new RoomAssetResponse(
                asset.getId(),
                asset.getRoom().getId(),
                asset.getAssetName(),
                asset.getAssetCategory(),
                asset.getQuantity(),
                asset.getCurrentCondition(),
                asset.getDescription(),
                imageFile != null ? imageFile.getId() : null
        );
    }

    private MeterReadingLatestResponse.Item toMeterReadingItem(MeterReadingEntity reading) {
        return new MeterReadingLatestResponse.Item(
                reading.getId(),
                reading.getMeter().getMeterType(),
                reading.getCurrentValue(),
                reading.getCurrentValue(),
                reading.getCurrentValue(),
                reading.getReadingPeriod(),
                reading.getReadingDate(),
                reading.getReadingDate(),
                reading.getCreatedAt()
        );
    }

    private String requireAssetName(RoomAssetRequest request) {
        String assetName = request.assetName();
        if (assetName == null || assetName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ten thiet bi la bat buoc.");
        }
        return assetName;
    }

    private Integer resolveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return 1;
        }
        return quantity;
    }

    private AssetCondition resolveCondition(AssetCondition condition) {
        return condition != null ? condition : AssetCondition.GOOD;
    }

    private FileMetadataEntity resolveFile(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "File anh khong ton tai."));
    }
}
