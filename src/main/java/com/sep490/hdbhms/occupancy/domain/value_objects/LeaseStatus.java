package com.sep490.hdbhms.occupancy.domain.value_objects;

public enum LeaseStatus {
    DRAFT,
    PENDING_SIGNATURE,
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    TERMINATION_PENDING,
    LIQUIDATED,
    RENEWED,
    AUTO_TERMINATED,
    CANCELLED
}
