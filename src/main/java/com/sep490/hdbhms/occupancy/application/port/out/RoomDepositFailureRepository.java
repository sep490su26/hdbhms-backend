package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.RoomDepositFailure;

import java.time.LocalDateTime;
import java.util.List;

public interface RoomDepositFailureRepository {
    RoomDepositFailure save(RoomDepositFailure failure);

    boolean existsByRoomHoldId(Long roomHoldId);

    List<RoomDepositFailure> findByRoomIdAndOccurredAtAfter(Long roomId, LocalDateTime occurredAfter);
}
