package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomDepositFailure;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomDepositFailureEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomDepositFailurePersistenceMapper {
    public RoomDepositFailure toDomain(RoomDepositFailureEntity entity) {
        if (entity == null) return null;
        return RoomDepositFailure.builder()
                .id(entity.getId())
                .roomId(entity.getRoomId())
                .roomHoldId(entity.getRoomHoldId())
                .paymentIntentId(entity.getPaymentIntentId())
                .reason(entity.getReason())
                .occurredAt(entity.getOccurredAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RoomDepositFailureEntity toEntity(RoomDepositFailure domain) {
        if (domain == null) return null;
        return RoomDepositFailureEntity.builder()
                .id(domain.getId())
                .roomId(domain.getRoomId())
                .roomHoldId(domain.getRoomHoldId())
                .paymentIntentId(domain.getPaymentIntentId())
                .reason(domain.getReason())
                .occurredAt(domain.getOccurredAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
