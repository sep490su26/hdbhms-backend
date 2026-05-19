package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomHold;

public interface CreateRoomHoldTaskPort {
    void execute(RoomHold roomHold);
}
