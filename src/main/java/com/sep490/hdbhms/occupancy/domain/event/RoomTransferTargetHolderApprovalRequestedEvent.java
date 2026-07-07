package com.sep490.hdbhms.occupancy.domain.event;

import java.time.LocalDate;

public record RoomTransferTargetHolderApprovalRequestedEvent(
        Long requestId,
        String requestCode,
        Long targetHolderUserId,
        Long requesterUserId,
        Long oldRoomId,
        Long targetRoomId,
        String oldRoomName,
        String targetRoomName,
        Long targetContractId,
        LocalDate requestedTransferDate
) {
}