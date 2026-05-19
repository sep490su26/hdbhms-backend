package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMaintenanceTicketAttachmentRepository extends JpaRepository<MaintenanceTicketAttachmentEntity, Long> {
}
