package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.MeterReading;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;

import java.util.Optional;

public interface MeterReadingRepository {
    MeterReading save(MeterReading reading);
    Optional<MeterReading> findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(Long roomId, MeterType meterType);
    Optional<MeterReading> findFirstByMeterIdOrderByReadingDateDesc(Long meterId);
    Optional<MeterReading> findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(Long meterId, String readingPeriod);
    Optional<MeterReading> findByMeterIdAndBatchId(Long meterId, Long batchId);
    java.util.List<MeterReading> findByMeterIdAndReadingDateBeforeOrderByReadingDateDesc(Long meterId, java.time.LocalDate readingDate);
}
