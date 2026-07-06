package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtNoticeTrackerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaDebtNoticeTrackerRepository extends JpaRepository<DebtNoticeTrackerEntity, Long> {
    Optional<DebtNoticeTrackerEntity> findByLeaseContract_Id(Long leaseContractId);
}
