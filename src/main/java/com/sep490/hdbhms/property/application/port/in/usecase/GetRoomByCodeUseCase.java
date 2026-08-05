package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.domain.model.Room;

public interface GetRoomByCodeUseCase {
    Room getRoomByCode(String roomCode);
}
