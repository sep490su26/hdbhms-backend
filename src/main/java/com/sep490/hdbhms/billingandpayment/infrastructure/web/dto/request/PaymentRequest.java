package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

public record PaymentRequest(String paymentId, Long amount, String returnUrl) {
}
