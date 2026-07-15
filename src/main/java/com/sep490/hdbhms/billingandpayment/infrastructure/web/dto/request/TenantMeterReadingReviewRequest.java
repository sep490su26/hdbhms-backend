package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

import java.math.BigDecimal;

public record TenantMeterReadingReviewRequest(
        BigDecimal reportedCurrentValue,
        String description,
        Long evidenceFileId
) {
}
