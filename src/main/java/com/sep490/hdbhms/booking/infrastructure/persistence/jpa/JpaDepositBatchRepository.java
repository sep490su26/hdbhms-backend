package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDepositBatchRepository extends JpaRepository<DepositBatchEntity, Long> {
}
