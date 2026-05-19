package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMaintenanceReviewRepository extends JpaRepository<MaintenanceReviewEntity, Long> {
}
