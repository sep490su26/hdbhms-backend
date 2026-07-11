package com.sep490.hdbhms.maintenance.infrastructure.persistence.jpa;

import com.sep490.hdbhms.maintenance.infrastructure.persistence.entity.MaintenanceCostEntity;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaMaintenanceCostRepository extends JpaRepository<MaintenanceCostEntity, Long> {
    List<MaintenanceCostEntity> findAllByTicket_IdOrderByCreatedAtAsc(Long ticketId);
    List<MaintenanceCostEntity> findAllByPaidByOrderByCreatedAtDesc(PaidBy paidBy);
}
