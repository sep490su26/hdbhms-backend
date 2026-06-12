package com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketPersistenceMapper {
    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaUserRepository jpaUserRepository;

    public MaintenanceTicket toDomain(MaintenanceTicketEntity entity) {
        if (entity == null) return null;
        return MaintenanceTicket.builder()
                .id(entity.getId())
                .ticketCode(entity.getTicketCode())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .ticketScope(entity.getTicketScope())
                .priority(entity.getPriority())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .rejectionReason(entity.getRejectionReason())
                .assignedToId(entity.getAssignedTo() != null ? entity.getAssignedTo().getId() : null)
                .workerName(entity.getWorkerName())
                .repairmanPhone(entity.getRepairmanPhone())
                .repairItems(entity.getRepairItems())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public MaintenanceTicketEntity toEntity(MaintenanceTicket domain) {
        if (domain == null) return null;
        return MaintenanceTicketEntity.builder()
                .id(domain.getId())
                .ticketCode(domain.getTicketCode())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.getReferenceById(domain.getPropertyId())
                        : null)
                .room(domain.getRoomId() != null
                        ? jpaRoomRepository.getReferenceById(domain.getRoomId())
                        : null)
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.getReferenceById(domain.getContractId())
                        : null)
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.getReferenceById(domain.getCreatedById())
                        : null)
                .ticketScope(domain.getTicketScope())
                .priority(domain.getPriority())
                .category(domain.getCategory())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .status(domain.getStatus())
                .rejectionReason(domain.getRejectionReason())
                .assignedTo(domain.getAssignedToId() != null
                        ? jpaUserRepository.getReferenceById(domain.getAssignedToId())
                        : null)
                .workerName(domain.getWorkerName())
                .repairmanPhone(domain.getRepairmanPhone())
                .repairItems(domain.getRepairItems())
                .completedAt(domain.getCompletedAt())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
