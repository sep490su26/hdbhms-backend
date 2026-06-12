package com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper;

import com.sep490.hdbhms.file.infrastructure.persistence.jpa.JpaFileMetadataRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketAttachment;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketAttachmentEntity;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MaintenanceTicketAttachmentPersistenceMapper {
    JpaMaintenanceTicketRepository jpaMaintenanceTicketRepository;
    JpaFileMetadataRepository jpaFileMetadataRepository;
    JpaUserRepository jpaUserRepository;

    public MaintenanceTicketAttachment toDomain(MaintenanceTicketAttachmentEntity entity) {
        if (entity == null) return null;
        return MaintenanceTicketAttachment.builder()
                .id(entity.getId())
                .ticketId(entity.getTicket() != null ? entity.getTicket().getId() : null)
                .fileId(entity.getFile() != null ? entity.getFile().getId() : null)
                .attachmentPhase(entity.getAttachmentPhase())
                .sortOrder(entity.getSortOrder())
                .createdById(entity.getCreatedByUser() != null
                        ? entity.getCreatedByUser().getId()
                        : entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public MaintenanceTicketAttachmentEntity toEntity(MaintenanceTicketAttachment domain) {
        if (domain == null) return null;
        return MaintenanceTicketAttachmentEntity.builder()
                .id(domain.getId())
                .ticket(domain.getTicketId() != null
                        ? jpaMaintenanceTicketRepository.getReferenceById(domain.getTicketId())
                        : null)
                .file(domain.getFileId() != null
                        ? jpaFileMetadataRepository.getReferenceById(domain.getFileId())
                        : null)
                .attachmentPhase(domain.getAttachmentPhase())
                .sortOrder(domain.getSortOrder())
                .createdByUser(domain.getCreatedById() != null
                        ? jpaUserRepository.getReferenceById(domain.getCreatedById())
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
