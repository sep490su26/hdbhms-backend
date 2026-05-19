package com.sep490.hdbhms.maintenance.application.port.out;

import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicketAttachment;

import java.util.Optional;

public interface MaintenanceTicketAttachmentRepository {
    MaintenanceTicketAttachment save(MaintenanceTicketAttachment maintenanceTicketAttachment);

    Optional<MaintenanceTicketAttachment> findById(Long id);
}
