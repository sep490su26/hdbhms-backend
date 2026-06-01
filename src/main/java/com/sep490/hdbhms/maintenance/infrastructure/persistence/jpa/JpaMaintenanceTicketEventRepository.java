package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMaintenanceTicketEventRepository extends JpaRepository<MaintenanceTicketEventEntity, Long> {
}
