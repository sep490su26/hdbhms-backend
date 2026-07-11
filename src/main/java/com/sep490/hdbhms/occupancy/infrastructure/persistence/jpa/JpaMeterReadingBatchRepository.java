package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;

import java.util.List;

import java.util.Optional;

public interface JpaMeterReadingBatchRepository extends JpaRepository<MeterReadingBatchEntity, Long> {
    List<MeterReadingBatchEntity> findByProperty_IdOrderByReadingPeriodDesc(Long propertyId);
    Optional<MeterReadingBatchEntity> findByProperty_IdAndReadingPeriod(Long propertyId, String readingPeriod);

    Optional<MeterReadingBatchEntity> findFirstByProperty_IdAndStatusInOrderByCreatedAtDesc(Long propertyId, List<BatchStatus> statuses);
}
