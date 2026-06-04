package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaMaintenanceReviewRepository extends JpaRepository<MaintenanceReviewEntity, Long> {
    Optional<MaintenanceReviewEntity> findByTicket_IdAndReviewerUser_Id(Long ticketId, Long reviewerUserId);
}
