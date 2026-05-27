package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    Room save(Room room);

    Optional<Room> findById(Long id);

    List<Room> findAllByPropertyIdAndFloorId(Long propertyId, Long floorId);

    Page<Room> findAll(List<Long> ids, RoomStatus status, Long minPrice, Long maxPrice, Pageable pageable);

    Optional<Room> findByRoomCode(String roomCode);

    int updateRoomStatusIfCurrent(Long roomId, RoomStatus expectedStatus, RoomStatus newStatus);
}
