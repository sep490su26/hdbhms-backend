package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.UtilityBillingRunItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaUtilityBillingRunItemRepository extends JpaRepository<UtilityBillingRunItemEntity, Long> {
    List<UtilityBillingRunItemEntity> findByRun_IdOrderByRoom_RoomCodeAscIdAsc(Long runId);

    Optional<UtilityBillingRunItemEntity> findByIdAndRun_Id(Long id, Long runId);

    void deleteByRun_Id(Long runId);
}
