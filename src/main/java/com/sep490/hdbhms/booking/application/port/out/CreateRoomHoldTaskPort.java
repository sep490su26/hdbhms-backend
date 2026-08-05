package com.sep490.hdbhms.booking.application.port.out;

import com.sep490.hdbhms.booking.domain.model.RoomHold;

public interface CreateRoomHoldTaskPort {
    void execute(RoomHold roomHold);
}
