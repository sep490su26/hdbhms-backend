package com.sep490.hdbhms.modules.maintenance.dto;


public record MaintenanceTicketActionResponse(
        Long id,
        String status,
        String statusLabel
) {
}
