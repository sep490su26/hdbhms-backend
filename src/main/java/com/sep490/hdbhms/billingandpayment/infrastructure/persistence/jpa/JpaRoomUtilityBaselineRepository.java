package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.jpa;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RoomUtilityBaselineEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaRoomUtilityBaselineRepository extends JpaRepository<RoomUtilityBaselineEntity, Long> {
    Optional<RoomUtilityBaselineEntity> findByMeter_Id(Long meterId);
}
