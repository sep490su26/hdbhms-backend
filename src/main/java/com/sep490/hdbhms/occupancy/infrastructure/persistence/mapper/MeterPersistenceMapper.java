package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Meter;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
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
public class MeterPersistenceMapper {

    JpaRoomRepository jpaRoomRepository;

    public Meter toDomain(MeterEntity entity) {
        if (entity == null) return null;
        return Meter.builder()
                .id(entity.getId())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .meterType(entity.getMeterType())
                .meterCode(entity.getMeterCode())
                .status(entity.getStatus())
                .installedAt(entity.getInstalledAt())
                .createdAt(entity.getCreatedAt())
                .activeMeterKey(entity.getActiveMeterKey())
                .build();
    }

    public MeterEntity toEntity(Meter domain) {
        if (domain == null) return null;
        return MeterEntity.builder()
                .id(domain.getId())
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.findById(domain.getRoomId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .meterType(domain.getMeterType())
                .meterCode(domain.getMeterCode())
                .status(domain.getStatus())
                .installedAt(domain.getInstalledAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
