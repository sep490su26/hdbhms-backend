package com.sep490.hdbhms.billingandpayment.domain.value_objects;

public enum DepositAgreementStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    CONVERTED_TO_LEASE,
    EXTENDED,
    REFUNDED,
    FORFEITED,
    CANCELLED
}