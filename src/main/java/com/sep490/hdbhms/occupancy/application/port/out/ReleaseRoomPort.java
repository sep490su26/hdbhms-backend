package com.sep490.hdbhms.occupancy.application.port.out;

public interface ReleaseRoomPort {
    void execute(Long roomId);

    void executeImmediately(Long roomId);
}
