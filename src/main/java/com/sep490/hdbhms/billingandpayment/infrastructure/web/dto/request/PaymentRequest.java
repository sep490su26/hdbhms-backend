package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

public record PaymentRequest(
        Long paymentId,
        Long amount,
        String description
) {
}
