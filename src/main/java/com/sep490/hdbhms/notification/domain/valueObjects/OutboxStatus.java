package com.sep490.hdbhms.notification.domain.valueObjects;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    DEAD_LETTER
}
