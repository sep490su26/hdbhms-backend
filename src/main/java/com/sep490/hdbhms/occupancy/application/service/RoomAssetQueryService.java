package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.application.port.in.query.RoomAssetQueryUseCase;
import com.sep490.hdbhms.occupancy.domain.model.RoomAsset;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomAssetRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.RoomAssetPersistenceMapper;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomAssetResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomAssetQueryService implements RoomAssetQueryUseCase {

    JpaRoomAssetRepository jpaRoomAssetRepository;
    RoomAssetPersistenceMapper roomAssetPersistenceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RoomAssetResponse> getRoomAssets(Long roomId) {
        return jpaRoomAssetRepository.findByRoom_IdAndDeletedAtIsNull(roomId).stream()
                .map(roomAssetPersistenceMapper::toDomain)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomAssetResponse getRoomAsset(Long roomId, Long assetId) {
        RoomAsset domain = jpaRoomAssetRepository.findByIdAndRoom_IdAndDeletedAtIsNull(assetId, roomId)
                .map(roomAssetPersistenceMapper::toDomain)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        return toResponse(domain);
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
