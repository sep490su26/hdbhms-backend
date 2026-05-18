package com.sep490.hdbhms.identityandaccess.domain.value_objects;

public enum LoginStatus {
    SUCCESS,
    INVALID_PASSWORD,
    ACCOUNT_LOCKED,
    ACCOUNT_EXPIRED,
    RATE_LIMITED
}
