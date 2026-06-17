package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.MeterReadingRepository;
import com.sep490.hdbhms.occupancy.domain.model.MeterReading;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaMeterReadingRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.MeterReadingPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataMeterReadingRepository implements MeterReadingRepository {

    JpaMeterReadingRepository jpaMeterReadingRepository;
    MeterReadingPersistenceMapper meterReadingPersistenceMapper;

    @Override
    public MeterReading save(MeterReading reading) {
        return meterReadingPersistenceMapper.toDomain(
                jpaMeterReadingRepository.save(
                        meterReadingPersistenceMapper.toEntity(reading)
                )
        );
    }

    @Override
    public Optional<MeterReading> findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(Long roomId, MeterType meterType) {
        return jpaMeterReadingRepository.findFirstByRoom_IdAndMeter_MeterTypeOrderByReadingDateDesc(roomId, meterType)
                .map(meterReadingPersistenceMapper::toDomain);
    }

    @Override
    public Optional<MeterReading> findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(Long meterId, String readingPeriod) {
        return jpaMeterReadingRepository.findFirstByMeter_IdAndReadingPeriodOrderByRevisionNoDesc(meterId, readingPeriod)
                .map(meterReadingPersistenceMapper::toDomain);
    }

    @Override
    public Optional<MeterReading> findByMeterIdAndBatchId(Long meterId, Long batchId) {
        return jpaMeterReadingRepository.findByMeter_IdAndBatchId(meterId, batchId)
                .map(meterReadingPersistenceMapper::toDomain);
    }

    @Override
    public java.util.List<MeterReading> findByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(Long meterId, java.time.LocalDate readingDate) {
        return jpaMeterReadingRepository.findByMeter_IdAndReadingDateBeforeOrderByReadingDateDesc(meterId, readingDate).stream()
                .map(meterReadingPersistenceMapper::toDomain).toList();
    }
}
