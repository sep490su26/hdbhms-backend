package com.sep490.hdbhms.scheduling.application.port.out;

public interface ExpireRoomHoldPort {
    void execute(Long roomHoldId);
}
