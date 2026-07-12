package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.usecase.ManageRoomAssetUseCase;
import com.sep490.hdbhms.occupancy.domain.model.RoomAsset;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomAssetEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomAssetPersistenceMapper;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.RoomAssetRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomAssetResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ManageRoomAssetService implements ManageRoomAssetUseCase {

    JpaRoomAssetRepository jpaRoomAssetRepository;
    RoomAssetPersistenceMapper roomAssetPersistenceMapper;

    @Override
    @Transactional
    public RoomAssetResponse createRoomAsset(Long roomId, RoomAssetRequest request) {
        RoomAsset domain = RoomAsset.builder()
                .roomId(roomId)
                .assetName(request.assetName())
                .assetCategory(request.assetCategory())
                .quantity(request.quantity())
                .currentCondition(request.currentCondition())
                .description(request.description())
                .fileImageId(request.fileImageId())
                .build();

        RoomAssetEntity entity = roomAssetPersistenceMapper.toEntity(domain);
        entity = jpaRoomAssetRepository.save(entity);
        return toResponse(roomAssetPersistenceMapper.toDomain(entity));
    }

    @Override
    @Transactional
    public RoomAssetResponse updateRoomAsset(Long roomId, Long assetId, RoomAssetRequest request) {
        RoomAssetEntity entity = getActiveAssetEntity(roomId, assetId);

        entity.setAssetName(request.assetName());
        entity.setAssetCategory(request.assetCategory());
        entity.setQuantity(request.quantity());
        entity.setCurrentCondition(request.currentCondition());
        entity.setDescription(request.description());

        // Update imageFile relation if provided
        if (request.fileImageId() != null) {
            RoomAsset updated = RoomAsset.builder()
                    .id(entity.getId())
                    .roomId(roomId)
                    .assetName(entity.getAssetName())
                    .assetCategory(entity.getAssetCategory())
                    .quantity(entity.getQuantity())
                    .currentCondition(entity.getCurrentCondition())
                    .description(entity.getDescription())
                    .fileImageId(request.fileImageId())
                    .createdAt(entity.getCreatedAt())
                    .build();
            entity = roomAssetPersistenceMapper.toEntity(updated);
        }

        entity = jpaRoomAssetRepository.save(entity);
        return toResponse(roomAssetPersistenceMapper.toDomain(entity));
    }

    @Override
    @Transactional
    public void deleteRoomAsset(Long roomId, Long assetId) {
        RoomAssetEntity entity = getActiveAssetEntity(roomId, assetId);
        entity.setDeletedAt(LocalDateTime.now());
        jpaRoomAssetRepository.save(entity);
    }

    private RoomAssetEntity getActiveAssetEntity(Long roomId, Long assetId) {
        return jpaRoomAssetRepository.findByIdAndRoom_IdAndDeletedAtIsNull(assetId, roomId)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
    }

    private RoomAssetResponse toResponse(RoomAsset domain) {
        return new RoomAssetResponse(
                domain.getId(),
                domain.getRoomId(),
                domain.getAssetName(),
                domain.getAssetCategory(),
                domain.getQuantity(),
                domain.getCurrentCondition(),
                domain.getDescription(),
                domain.getFileImageId()
        );
    }
}
