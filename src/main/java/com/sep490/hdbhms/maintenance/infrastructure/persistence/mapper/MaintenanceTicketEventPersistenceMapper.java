package com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketEvent;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEventEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketEventPersistenceMapper {
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaUserRepository jpaUserRepository;

    public MaintenanceTicketEvent toDomain(MaintenanceTicketEventEntity entity) {
        if (entity == null) return null;
        return MaintenanceTicketEvent.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .fromStatus(entity.getFromStatus())
                .toStatus(entity.getToStatus())
                .action(entity.getAction())
                .note(entity.getNote())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MaintenanceTicketEventEntity toEntity(MaintenanceTicketEvent domain) {
        if (domain == null) return null;
        return MaintenanceTicketEventEntity.builder()
                .id(domain.getId())
                .ticket(domain.getTicketId() != null
                        ? jpaMaintenanceTicketRepository.getReferenceById(domain.getTicketId())
                        : null)
                .fromStatus(domain.getFromStatus())
                .toStatus(domain.getToStatus())
                .action(domain.getAction())
                .note(domain.getNote())
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.getReferenceById(domain.getCreatedById())
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
