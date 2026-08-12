package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import lombok.Getter;

@Getter
public enum ModificationType {
    PASSWORD_CHANGE("Đổi mật khẩu"),
    PASSWORD_RESET("Đặt lại mật khẩu"),
    ROLE("Vai trò"),
    EMAIL("Email"),
    STATUS("Trạng thái");

    private final String displayName;

    ModificationType(final String displayName) {
        this.displayName = displayName;
    }
}
