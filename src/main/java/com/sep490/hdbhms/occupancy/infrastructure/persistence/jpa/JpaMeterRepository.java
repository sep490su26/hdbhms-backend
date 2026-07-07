package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterEntity;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaMeterRepository extends JpaRepository<MeterEntity, Long> {
    Optional<MeterEntity> findFirstByRoom_IdAndMeterTypeAndStatus(Long roomId, MeterType meterType, MeterStatus status);
}
