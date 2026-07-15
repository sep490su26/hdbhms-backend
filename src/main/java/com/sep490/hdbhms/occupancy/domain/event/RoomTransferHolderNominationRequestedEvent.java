package com.sep490.hdbhms.occupancy.domain.event;

import java.time.LocalDate;

public record RoomTransferHolderNominationRequestedEvent(
        Long requestId,
        String requestCode,
        Long nominatedHolderUserId,
        Long nominatorUserId,
        Long nominatedHolderProfileId,
        Long oldRoomId,
        Long targetRoomId,
        String oldRoomName,
        String targetRoomName,
        LocalDate requestedTransferDate
) {
}
