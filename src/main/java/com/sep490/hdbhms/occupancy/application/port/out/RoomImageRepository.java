package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomImage;

import java.util.List;
import java.util.Optional;

public interface RoomImageRepository {
    RoomImage save(RoomImage roomImage);

    Optional<RoomImage> findById(Long id);

    List<RoomImage> findAllByRoomId(Long roomId);
}
