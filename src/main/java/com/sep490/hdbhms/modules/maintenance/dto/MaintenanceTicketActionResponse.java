package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MaintenanceTicketActionResponse(
        Long id,
        String status,

        @JsonProperty("status_label")
        String statusLabel
) {
}
