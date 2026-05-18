package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeterReadingRepository extends JpaRepository<MeterReadingEntity, Long> {
}
