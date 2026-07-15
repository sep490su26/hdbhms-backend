package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

public record ManualPaymentRequest(
        Long amount,
        String payerName,
        String note
) {
}
