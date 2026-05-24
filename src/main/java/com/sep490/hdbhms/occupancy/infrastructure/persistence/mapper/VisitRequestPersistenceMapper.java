package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.VisitRequestEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeadRepository;
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
    JpaLeadRepository jpaLeadRepository;
    JpaUserRepository jpaUserRepository;

    public VisitRequest toDomain(VisitRequestEntity entity) {
        if (entity == null) return null;
        return VisitRequest.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .leadId(entity.getLead() != null ? entity.getLead().getId() : null)
                .visitorName(entity.getVisitorName())
                .visitorPhone(entity.getVisitorPhone())
                .visitorEmail(entity.getVisitorEmail())
                .preferredStart(entity.getPreferredStart())
                .preferredEnd(entity.getPreferredEnd())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
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
                .lead(domain.getLeadId() != null
                        ? jpaLeadRepository.getReferenceById(domain.getLeadId())
                        : null)
                .visitorName(domain.getVisitorName())
                .visitorPhone(domain.getVisitorPhone())
                .visitorEmail(domain.getVisitorEmail())
                .preferredStart(domain.getPreferredStart())
                .preferredEnd(domain.getPreferredEnd())
                .status(domain.getStatus())
                .notes(domain.getNotes())
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.getReferenceById(domain.getCreatedById())
                        : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
