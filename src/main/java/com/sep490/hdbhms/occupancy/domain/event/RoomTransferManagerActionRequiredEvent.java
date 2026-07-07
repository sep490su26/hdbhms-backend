package com.sep490.hdbhms.occupancy.domain.event;

import java.time.LocalDate;
import java.util.List;

public record RoomTransferManagerActionRequiredEvent(
        Long requestId,
        String requestCode,
        List<Long> managerUserIds,
        String actionType,
        String actionLabel,
        Long oldRoomId,
        Long targetRoomId,
        String oldRoomName,
        String targetRoomName,
        LocalDate requestedTransferDate
) {
}
