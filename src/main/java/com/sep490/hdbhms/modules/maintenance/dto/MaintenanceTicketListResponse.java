package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceTicketListResponse(
        List<Item> items,
        int page,
        int size,
        long total,

        @JsonProperty("empty_message")
        String emptyMessage
) {
    public record Item(
            Long id,

            @JsonProperty("ticket_code")
            String ticketCode,

            @JsonProperty("display_code")
            String displayCode,

            @JsonProperty("room_id")
            Long roomId,

            @JsonProperty("room_code")
            String roomCode,

            String title,
            String description,

            @JsonProperty("short_description")
            String shortDescription,

            @JsonProperty("description_short")
            String descriptionShort,

            @JsonProperty("ticket_scope")
            String ticketScope,

            String category,

            @JsonProperty("category_label")
            String categoryLabel,

            String priority,
            String status,

            @JsonProperty("status_label")
            String statusLabel,

            @JsonProperty("rejected_reason")
            String rejectedReason,

            @JsonProperty("created_at")
            OffsetDateTime createdAt,

            @JsonProperty("created_date")
            String createdDate,

            @JsonProperty("attachment_count")
            long attachmentCount
    ) {
    }
}
