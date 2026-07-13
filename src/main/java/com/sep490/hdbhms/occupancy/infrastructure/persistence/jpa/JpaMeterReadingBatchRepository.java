package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaMeterReadingBatchRepository extends JpaRepository<MeterReadingBatchEntity, Long> {
    List<MeterReadingBatchEntity> findByProperty_IdOrderByReadingPeriodDescIdDesc(Long propertyId);
    List<MeterReadingBatchEntity> findAllByProperty_IdAndReadingPeriodOrderByIdDesc(Long propertyId, String readingPeriod);

    Optional<MeterReadingBatchEntity> findFirstByProperty_IdAndStatusInOrderByCreatedAtDesc(Long propertyId, List<BatchStatus> statuses);
}
