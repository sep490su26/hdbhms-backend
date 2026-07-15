package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

public record DepositDashboardSummaryResponse(
        long totalHeldAmount,
        long heldCount,
        long convertedCount
) {
}
