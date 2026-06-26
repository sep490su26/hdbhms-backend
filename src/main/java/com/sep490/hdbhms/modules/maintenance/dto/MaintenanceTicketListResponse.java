package com.sep490.hdbhms.modules.maintenance.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record MaintenanceTicketListResponse(
        List<Item> items,
        int page,
        int size,
        long total,
        String emptyMessage
) {
    public record Item(
            Long id,
            String ticketCode,
            String displayCode,
            Long roomId,
            String roomCode,

            String title,
            String description,
            String shortDescription,
            String descriptionShort,
            String ticketScope,

            String category,
            String categoryLabel,

            String priority,
            String status,
            String statusLabel,
            String rejectedReason,
            OffsetDateTime createdAt,
            String createdDate,
            long attachmentCount
    ) {
    }
}
