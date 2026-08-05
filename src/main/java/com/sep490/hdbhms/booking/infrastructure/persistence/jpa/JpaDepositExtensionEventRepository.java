package com.sep490.hdbhms.booking.infrastructure.persistence.jpa;

import com.sep490.hdbhms.booking.infrastructure.persistence.entity.DepositExtensionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDepositExtensionEventRepository extends JpaRepository<DepositExtensionEventEntity, Long> {
}
