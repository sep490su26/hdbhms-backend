package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomHold;

import java.util.Optional;

public interface RoomHoldRepository {
    RoomHold save(RoomHold roomHold);

    Optional<RoomHold> findById(Long id);
}
