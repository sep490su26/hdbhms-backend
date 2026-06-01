package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.RoomStatusDisplayConfig;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomStatusDisplayConfigEntity;
import org.springframework.stereotype.Component;

@Component
public class RoomStatusDisplayConfigPersistenceMapper {
    public RoomStatusDisplayConfig toDomain(RoomStatusDisplayConfigEntity entity) {
        if (entity == null) return null;
        return RoomStatusDisplayConfig.builder()
                .id(entity.getId())
                .roomStatus(entity.getRoomStatus())
                .colorHex(entity.getColorHex())
                .label(entity.getLabel())
                .build();
    }

    public RoomStatusDisplayConfigEntity toEntity(RoomStatusDisplayConfig domain) {
        if (domain == null) return null;
        return RoomStatusDisplayConfigEntity.builder()
                .id(domain.getId())
                .roomStatus(domain.getRoomStatus())
                .colorHex(domain.getColorHex())
                .label(domain.getLabel())
                .build();
    }
}
