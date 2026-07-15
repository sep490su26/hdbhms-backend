package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.occupancy.domain.model.RoomAsset;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAssetPersistenceMapper {

    JpaRoomRepository jpaRoomRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;

    public RoomAsset toDomain(RoomAssetEntity entity) {
        if (entity == null) return null;
        return RoomAsset.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .assetName(entity.getAssetName())
                .assetCategory(entity.getAssetCategory())
                .quantity(entity.getQuantity())
                .currentCondition(entity.getCurrentCondition())
                .description(entity.getDescription())
                .fileImageId(entity.getImageFile() != null ? entity.getImageFile().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public RoomAssetEntity toEntity(RoomAsset domain) {
        if (domain == null) return null;
        return RoomAssetEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_ASSET_NOT_FOUND))
                        : null)
                .assetName(domain.getAssetName())
                .assetCategory(domain.getAssetCategory())
                .quantity(domain.getQuantity())
                .currentCondition(domain.getCurrentCondition())
                .description(domain.getDescription())
                .imageFile(domain.getFileImageId() != null
                        ? jpaFileMetadataRepository.findById(domain.getFileImageId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.ROOM_ASSET_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
