package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import lombok.Getter;

@Getter
public enum ModificationType {
    PASSWORD_CHANGE("Password change"),
    PASSWORD_RESET("Password reset"),
    ROLE("Role"),
    EMAIL("Email"),
    STATUS("Status");

    private final String displayName;

    ModificationType(final String displayName) {
        this.displayName = displayName;
    }
}
