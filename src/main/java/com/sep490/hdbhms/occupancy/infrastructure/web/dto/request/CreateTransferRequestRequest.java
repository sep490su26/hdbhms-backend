package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateTransferRequestRequest(
        @NotNull(message = "Source contract ID is required")
        Long sourceContractId,

        @NotNull(message = "Target room ID is required")
        Long targetRoomId,

        @FutureOrPresent
        @NotNull(message = "Requested transfer date is required")
        LocalDate requestedTransferDate,

        List<Long> transferredTenantProfileIds,

        String reason
) {
}
