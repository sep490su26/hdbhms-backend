package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

public record TenantMeterReadingReviewResponse(
        Long id,
        String requestCode,
        String status
) {
}
