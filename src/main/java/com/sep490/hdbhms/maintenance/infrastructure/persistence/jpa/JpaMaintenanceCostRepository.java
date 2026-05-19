package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceCostEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMaintenanceCostRepository extends JpaRepository<MaintenanceCostEntity, Long> {
}
