package com.sep490.hdbhms.changerequest.infrastructure.web.dto.response;

import java.util.Map;

public record ChangeRequestStatsResponse(
        long pendingApproval,
        long approvedToday,
        long rejectedToday,
        long thisMonthTotal,
        Map<String, Long> requestTypeBreakdown
) {}
