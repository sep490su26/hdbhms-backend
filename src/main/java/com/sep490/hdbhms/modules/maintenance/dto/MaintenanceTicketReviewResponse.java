package com.sep490.hdbhms.modules.maintenance.dto;

import java.time.OffsetDateTime;

public record MaintenanceTicketReviewResponse(
        Long id,
        Long ticketId,

        int rating,
        String comment,
        OffsetDateTime createdAt
) {
}
