package com.sep490.hdbhms.booking.infrastructure.web.dto.response;

public record DepositDashboardSummaryResponse(
        long totalHeldAmount,
        long heldCount,
        long convertedCount
) {
}
