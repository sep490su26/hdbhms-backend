package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceTicketResponse(
        Long id,

        @JsonProperty("ticket_code")
        String ticketCode,

        String status,

        @JsonProperty("status_label")
        String statusLabel,

        @JsonProperty("room_id")
        Long roomId,

        @JsonProperty("room_code")
        String roomCode,

        String title,
        String description,
        String priority,

        @JsonProperty("ticket_scope")
        String ticketScope,

        List<Attachment> attachments,

        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
    public record Attachment(
            Long id,

            @JsonProperty("file_id")
            Long fileId,

            @JsonProperty("file_url")
            String fileUrl,

            String phase,

            @JsonProperty("sort_order")
            int sortOrder
    ) {
    }
}
