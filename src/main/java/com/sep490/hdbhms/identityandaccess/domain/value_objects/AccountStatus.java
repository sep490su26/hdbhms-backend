package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import lombok.Getter;

@Getter
public enum AccountStatus {
    PENDING_CONTRACT("Pending Contract"),
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    REJECTED("Rejected"),
    CLOSED("Closed"),
    ARCHIVED("Archived");
    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }
}
