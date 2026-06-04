package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record DepositRoomHoldStatusResponse(
        boolean canBook,
        String roomStatus,
        String holdStatus,
        LocalDateTime holdExpiresAt,
        long remainingSeconds,
        String message
) {
}
