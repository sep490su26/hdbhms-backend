package com.sep490.hdbhms.property.application.port.out;

import com.sep490.hdbhms.property.domain.model.Meter;
import com.sep490.hdbhms.property.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.property.domain.value_objects.MeterType;

import java.util.Optional;

public interface MeterRepository {
    Optional<Meter> findFirstByRoomIdAndMeterTypeAndStatus(Long roomId, MeterType meterType, MeterStatus status);
}
