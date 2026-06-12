package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.MeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.domain.model.MeterReadingBatch;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingBatchRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.MeterReadingBatchPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataMeterReadingBatchRepository implements MeterReadingBatchRepository {

    JpaMeterReadingBatchRepository jpaMeterReadingBatchRepository;
    MeterReadingBatchPersistenceMapper meterReadingBatchPersistenceMapper;

    @Override
    public MeterReadingBatch save(MeterReadingBatch batch) {
        return meterReadingBatchPersistenceMapper.toDomain(
                jpaMeterReadingBatchRepository.save(
                        meterReadingBatchPersistenceMapper.toEntity(batch)
                )
        );
    }
}
