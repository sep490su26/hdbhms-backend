package com.sep490.hdbhms.notification.domain.value_objects;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    DEAD_LETTER
}
