package com.sep490.hdbhms.modules.maintenance.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceTicketDetailResponse(
        Long id,
        String ticketCode,

        String status,
        String statusLabel,
        Long roomId,
        String roomCode,
        Long propertyId,
        String propertyName,

        String title,
        String description,
        String priority,
        String ticketScope,
        OffsetDateTime createdAt,
        UserSummary createdBy,
        List<Attachment> beforeAttachments,
        List<Attachment> afterAttachments,
        RepairInfo repairInfo,

        Review review,
        List<Event> events
) {
    public record UserSummary(
            Long id,
            String fullName
    ) {
    }

    public record Attachment(
            Long id,
            Long fileId,
            String fileUrl,
            String mimeType,

            String phase,
            int sortOrder
    ) {
    }

    public record RepairInfo(
            String workerName,
            String repairItems,
            String completionNote,
            String totalCost,
            OffsetDateTime completedAt
    ) {
    }

    public record Review(
            int rating,
            String comment,
            OffsetDateTime createdAt
    ) {
    }

    public record Event(
            Long id,
            String status,
            String title,
            String description,
            OffsetDateTime createdAt
    ) {
    }
}
