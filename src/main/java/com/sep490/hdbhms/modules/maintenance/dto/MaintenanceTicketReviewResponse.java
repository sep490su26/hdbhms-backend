package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record MaintenanceTicketReviewResponse(
        Long id,

        @JsonProperty("ticket_id")
        Long ticketId,

        int rating,
        String comment,

        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
}
