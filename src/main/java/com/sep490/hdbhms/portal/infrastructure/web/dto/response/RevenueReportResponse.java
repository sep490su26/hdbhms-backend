package com.sep490.hdbhms.portal.infrastructure.web.dto.response;

import java.util.List;

public record RevenueReportResponse(
        String periodType,
        String endPeriod,
        Long totalRevenue,
        Long previousTotalRevenue,
        Double revenueGrowthPercent,
        List<RevenuePeriodResponse> periods,
        List<RevenueSourceResponse> sources
) {
    public record RevenuePeriodResponse(
            String period,
            String label,
            Long room,
            Long utilities,
            Long service,
            Long extra,
            Long total,
            Long previous
    ) {
    }

    public record RevenueSourceResponse(
            String key,
            Long amount,
            Integer percent
    ) {
    }
}
