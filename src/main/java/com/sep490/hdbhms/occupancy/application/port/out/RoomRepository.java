package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Room;

import java.util.Optional;

public interface RoomRepository {
    Room save(Room room);

    Optional<Room> findById(Long id);
}
