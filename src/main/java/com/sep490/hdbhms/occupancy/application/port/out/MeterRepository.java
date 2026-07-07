package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Meter;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.MeterType;

import java.util.Optional;

public interface MeterRepository {
    Optional<Meter> findFirstByRoomIdAndMeterTypeAndStatus(Long roomId, MeterType meterType, MeterStatus status);
}
