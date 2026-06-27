package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

import java.time.LocalDateTime;

public record PaymentRequest(
        Long paymentId,
        Long amount,
        String description,
        LocalDateTime expiresAt
) {
    public PaymentRequest(Long paymentId, Long amount, String description) {
        this(paymentId, amount, description, null);
    }
}
