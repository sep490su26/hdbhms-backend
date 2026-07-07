package com.sep490.hdbhms.billingandpayment.domain.valueObjects;

public enum PaymentIntentStatus {
    CREATED,
    PENDING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    CANCELLED,
    REFUND_REQUIRED,
}
