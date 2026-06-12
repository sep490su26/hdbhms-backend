package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.MeterReadingBatch;

public interface MeterReadingBatchRepository {
    MeterReadingBatch save(MeterReadingBatch batch);
}
