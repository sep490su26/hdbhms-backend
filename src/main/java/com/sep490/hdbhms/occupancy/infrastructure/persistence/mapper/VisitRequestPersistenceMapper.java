package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestPersistenceMapper {
    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;

    public VisitRequest toDomain(VisitRequestEntity entity) {
        if (entity == null) return null;
        return VisitRequest.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .visitorName(entity.getVisitorName())
                .visitorPhone(entity.getVisitorPhone())
                .visitorEmail(entity.getVisitorEmail())
                .preferredStart(entity.getPreferredStart())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public VisitRequestEntity toEntity(VisitRequest domain) {
        if (domain == null) return null;
        return VisitRequestEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.getReferenceById(domain.getPropertyId())
                        : null)
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.getReferenceById(domain.getRoomId())
                        : null)
                .visitorName(domain.getVisitorName())
                .visitorPhone(domain.getVisitorPhone())
                .visitorEmail(domain.getVisitorEmail())
                .preferredStart(domain.getPreferredStart())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
