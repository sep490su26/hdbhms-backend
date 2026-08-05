package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDepositFormRepository extends JpaRepository<DepositFormEntity, Long> {
}
