package com.sep490.hdbhms.maintenance.infrastructure.persistence.repository;

import com.sep490.hdbhms.maintenance.application.port.out.MaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketAttachment;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa.JpaMaintenanceTicketAttachmentRepository;
import com.sep490.hdbhms.maintenance.infrastructure.persistence.mapper.MaintenanceTicketAttachmentPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataMaintenanceTicketAttachmentRepository implements MaintenanceTicketAttachmentRepository {
    JpaMaintenanceTicketAttachmentRepository jpaMaintenanceTicketAttachmentRepository;
    MaintenanceTicketAttachmentPersistenceMapper maintenanceTicketAttachmentPersistenceMapper;

    @Override
    public MaintenanceTicketAttachment save(MaintenanceTicketAttachment maintenanceTicketAttachment) {
        return maintenanceTicketAttachmentPersistenceMapper.toDomain(
                jpaMaintenanceTicketAttachmentRepository.save(
                        maintenanceTicketAttachmentPersistenceMapper.toEntity(
                                maintenanceTicketAttachment
                        )
                )
        );
    }

    @Override
    public Optional<MaintenanceTicketAttachment> findById(Long id) {
        return jpaMaintenanceTicketAttachmentRepository.findById(id)
                .map(maintenanceTicketAttachmentPersistenceMapper::toDomain);
    }
}
