package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.domain.model.Room;

public interface GetRoomByCodeUseCase {
    Room getRoomByCode(String roomCode);
}
