package com.sep490.hdbhms.booking.application.port.out;

public interface EarlyCancelRoomHoldTaskPort {
    void execute(Long roomHoldId);
}
