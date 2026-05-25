package com.sep490.hdbhms.modules.maintenance.dto;

public record ConfirmAndReviewResponse(
        MaintenanceTicketActionResponse ticket,
        MaintenanceTicketReviewResponse review
) {
}
