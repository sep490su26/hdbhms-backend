package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface JpaMaintenanceTicketAttachmentRepository extends JpaRepository<MaintenanceTicketAttachmentEntity, Long> {
    @EntityGraph(attributePaths = {"file"})
    List<MaintenanceTicketAttachmentEntity> findAllByTicket_IdOrderBySortOrderAsc(Long ticketId);
}
