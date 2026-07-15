package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.MeterReadingBatch;

import java.util.List;
import java.util.Optional;

public interface MeterReadingBatchRepository {
    MeterReadingBatch save(MeterReadingBatch batch);

    List<MeterReadingBatch> findByPropertyIdAndReadingPeriodOrderByIdDesc(Long propertyId, String readingPeriod);

    Optional<MeterReadingBatch> findById(Long batchId);
}
