package com.sep490.hdbhms.accounting.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record ExpenseTimelineResponse(
        String fromStatus,
        String toStatus,
        String note,
        Long actedBy,
        LocalDateTime actedAt
) {
}
