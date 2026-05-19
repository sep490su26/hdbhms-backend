package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMaintenanceTicketRepository extends JpaRepository<MaintenanceTicketEntity, Long> {
}
