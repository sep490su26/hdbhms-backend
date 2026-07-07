package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomHold;
import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomHoldStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RoomHoldRepository {
    RoomHold save(RoomHold roomHold);

    Optional<RoomHold> findById(Long id);

    boolean existsByRoomIdAndStatusIn(Long roomId, List<RoomHoldStatus> active);

    List<RoomHold> findExpiredUnconfirmedHolds(LocalDateTime now);

    Optional<RoomHold> findActiveHoldByRoomId(Long roomId, LocalDateTime now);
}
