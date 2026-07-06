package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.ManagerTaskStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.ManagerTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaManagerTaskRepository extends JpaRepository<ManagerTaskEntity, Long> {
    Optional<ManagerTaskEntity> findFirstByLeaseContract_IdAndStatusOrderByIdDesc(
            Long leaseContractId,
            ManagerTaskStatus status
    );
}
