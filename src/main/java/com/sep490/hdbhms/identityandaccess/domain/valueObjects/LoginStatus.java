package com.sep490.hdbhms.identityandaccess.domain.valueObjects;

public enum LoginStatus {
    SUCCESS,
    INVALID_PASSWORD,
    ACCOUNT_LOCKED,
    ACCOUNT_EXPIRED,
    RATE_LIMITED
}
