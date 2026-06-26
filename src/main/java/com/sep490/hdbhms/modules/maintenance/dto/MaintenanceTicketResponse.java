package com.sep490.hdbhms.modules.maintenance.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceTicketResponse(
        Long id,
        String ticketCode,

        String status,
        String statusLabel,
        Long roomId,
        String roomCode,

        String title,
        String description,
        String priority,
        String ticketScope,

        List<Attachment> attachments,
        OffsetDateTime createdAt
) {
    public record Attachment(
            Long id,
            Long fileId,
            String fileUrl,

            String phase,
            int sortOrder
    ) {
    }
}
