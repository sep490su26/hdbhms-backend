package com.sep490.hdbhms.occupancy.application.port.out;

public interface EarlyCancelRoomHoldTaskPort {
    void execute(Long roomHoldId);
}
