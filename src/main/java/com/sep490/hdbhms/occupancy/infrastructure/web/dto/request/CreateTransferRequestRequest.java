package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateTransferRequestRequest(
        @NotNull(message = "Source contract ID is required")
        Long sourceContractId,

        @NotNull(message = "Target room ID is required")
        Long targetRoomId,

        LocalDate requestedTransferDate,

        @NotNull(message = "Expected transfer date is required")
        LocalDate expectedTransferDate,

        List<Long> transferredTenantProfileIds,

        String reason
) {
    public CreateTransferRequestRequest {
        if (expectedTransferDate == null) {
            expectedTransferDate = requestedTransferDate;
        }
    }
}
