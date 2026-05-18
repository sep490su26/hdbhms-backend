package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.DebtSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDebtSnapshotRepository extends JpaRepository<DebtSnapshotEntity, Long> {
}
