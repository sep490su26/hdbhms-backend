package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.MeterReading;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;

import java.util.Optional;

public interface MeterReadingRepository {
    MeterReading save(MeterReading reading);
    Optional<MeterReading> findFirstByRoomIdAndMeterTypeOrderByReadingDateDesc(Long roomId, MeterType meterType);
    Optional<MeterReading> findFirstByMeterIdAndReadingPeriodOrderByRevisionNoDesc(Long meterId, String readingPeriod);
}
