package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMeterReadingBatchRepository extends JpaRepository<MeterReadingBatchEntity, Long> {
}
