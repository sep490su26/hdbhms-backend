package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomPersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;
    JpaFloorRepository jpaFloorRepository;

    public Room toDomain(RoomEntity entity) {
        if (entity == null) return null;
        return Room.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .floorId(entity.getFloor() != null ? entity.getFloor().getId() : null)
                .roomCode(entity.getRoomCode())
                .name(entity.getName())
                .areaM2(entity.getAreaM2())
                .listedPrice(entity.getListedPrice())
                .currentStatus(entity.getCurrentStatus())
                .maxOccupants(entity.getMaxOccupants())
                .publicNote(entity.getPublicNote())
                .internalNote(entity.getInternalNote())
                .positionX(entity.getPositionX())
                .positionY(entity.getPositionY())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public RoomEntity toEntity(Room domain) {
        if (domain == null) return null;
        return RoomEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .floor(domain.getFloorId() != null
                        ? jpaFloorRepository.findById(domain.getFloorId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .roomCode(domain.getRoomCode())
                .name(domain.getName())
                .areaM2(domain.getAreaM2())
                .listedPrice(domain.getListedPrice())
                .currentStatus(domain.getCurrentStatus())
                .maxOccupants(domain.getMaxOccupants())
                .publicNote(domain.getPublicNote())
                .internalNote(domain.getInternalNote())
                .positionX(domain.getPositionX())
                .positionY(domain.getPositionY())
                .sortOrder(domain.getSortOrder())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .version(domain.getVersion())
                .build();
    }
}
