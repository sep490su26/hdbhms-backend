package com.sep490.hdbhms.property.infrastructure.persistence.jpa;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.MeterReadingAnomalyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface JpaMeterReadingAnomalyRepository extends JpaRepository<MeterReadingAnomalyEntity, Long> {
    void deleteByMeterReading_IdAndResolvedAtIsNull(Long meterReadingId);

    long countByBatch_IdAndResolvedAtIsNull(Long batchId);

    List<MeterReadingAnomalyEntity> findByMeterReading_IdInAndResolvedAtIsNullOrderByIdAsc(Collection<Long> readingIds);

    List<MeterReadingAnomalyEntity> findByBatch_IdAndMeterReading_Room_IdAndResolvedAtIsNullOrderByIdAsc(
            Long batchId,
            Long roomId
    );
}
