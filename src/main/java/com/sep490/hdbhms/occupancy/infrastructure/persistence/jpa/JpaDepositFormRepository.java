package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositFormEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDepositFormRepository extends JpaRepository<DepositFormEntity, Long> {
}
