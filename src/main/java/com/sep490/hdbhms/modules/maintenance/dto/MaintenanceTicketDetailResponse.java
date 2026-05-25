package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceTicketDetailResponse(
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

        @JsonProperty("property_id")
        Long propertyId,

        @JsonProperty("property_name")
        String propertyName,

        String title,
        String description,
        String priority,

        @JsonProperty("ticket_scope")
        String ticketScope,

        @JsonProperty("created_at")
        OffsetDateTime createdAt,

        @JsonProperty("created_by")
        UserSummary createdBy,

        @JsonProperty("before_attachments")
        List<Attachment> beforeAttachments,

        @JsonProperty("after_attachments")
        List<Attachment> afterAttachments,

        @JsonProperty("repair_info")
        RepairInfo repairInfo,

        Review review,
        List<Event> events
) {
    public record UserSummary(
            Long id,

            @JsonProperty("full_name")
            String fullName
    ) {
    }

    public record Attachment(
            Long id,

            @JsonProperty("file_id")
            Long fileId,

            @JsonProperty("file_url")
            String fileUrl,

            @JsonProperty("mime_type")
            String mimeType,

            String phase,

            @JsonProperty("sort_order")
            int sortOrder
    ) {
    }

    public record RepairInfo(
            @JsonProperty("worker_name")
            String workerName,

            @JsonProperty("repair_items")
            String repairItems,

            @JsonProperty("completion_note")
            String completionNote,

            @JsonProperty("total_cost")
            String totalCost,

            @JsonProperty("completed_at")
            OffsetDateTime completedAt
    ) {
    }

    public record Review(
            int rating,
            String comment,

            @JsonProperty("created_at")
            OffsetDateTime createdAt
    ) {
    }

    public record Event(
            Long id,
            String status,
            String title,
            String description,

            @JsonProperty("created_at")
            OffsetDateTime createdAt
    ) {
    }
}
