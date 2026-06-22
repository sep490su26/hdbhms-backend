package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record InternalMaintenanceCostResponse(
        Long ticketId,
        String ticketCode,
        Long propertyId,
        String propertyName,
        Long roomId,
        String roomCode,
        String category,
        String ticketStatus,
        Long amount,
        String payer,
        String billingStatus,
        String accountingNote,
        LocalDateTime recordedAt
) {
}
