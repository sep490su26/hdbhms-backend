package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositExtensionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDepositExtensionEventRepository extends JpaRepository<DepositExtensionEventEntity, Long> {
}
