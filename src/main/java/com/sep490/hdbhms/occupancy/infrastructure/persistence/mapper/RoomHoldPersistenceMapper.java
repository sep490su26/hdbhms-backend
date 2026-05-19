package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomHoldEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTenantRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomHoldPersistenceMapper {
    JpaRoomRepository jpaRoomRepository;
    JpaTenantRepository jpaTenantRepository;

    public RoomHold toDomain(RoomHoldEntity entity) {
        if (entity == null) return null;
        return RoomHold.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .status(entity.getStatus())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .releasedAt(entity.getReleasedAt())
                .activeRoomKey(entity.getActiveRoomKey())
                .build();
    }

    public RoomHoldEntity toEntity(RoomHold domain) {
        if (domain == null) return null;
        return RoomHoldEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .tenant(domain.getTenantId() != null
                        ? jpaTenantRepository.findById(domain.getTenantId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .status(domain.getStatus())
                .expiresAt(domain.getExpiresAt())
                .createdAt(domain.getCreatedAt())
                .releasedAt(domain.getReleasedAt())
                .build();
    }
}
