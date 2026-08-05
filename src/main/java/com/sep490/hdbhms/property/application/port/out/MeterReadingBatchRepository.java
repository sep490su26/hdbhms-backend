package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.MeterReadingBatch;

import java.util.List;
import java.util.Optional;

public interface MeterReadingBatchRepository {
    MeterReadingBatch save(MeterReadingBatch batch);

    List<MeterReadingBatch> findByPropertyIdAndReadingPeriodOrderByIdDesc(Long propertyId, String readingPeriod);

    Optional<MeterReadingBatch> findById(Long batchId);
}
