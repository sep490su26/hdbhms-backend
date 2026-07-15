package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

public record UtilityBillingItemAdjustmentRequest(
        Long discountAmount,
        String adjustmentReason
) {
}
