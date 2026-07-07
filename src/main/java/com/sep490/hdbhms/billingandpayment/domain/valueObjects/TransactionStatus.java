package com.sep490.hdbhms.billingandpayment.domain.valueObjects;

public enum TransactionStatus {
    PENDING_RECONCILE,
    MATCHED,
    PARTIALLY_ALLOCATED,
    ALLOCATED,
    DUPLICATE,
    REJECTED,
    REFUNDED
}
