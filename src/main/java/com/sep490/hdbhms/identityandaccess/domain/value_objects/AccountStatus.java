package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import lombok.Getter;

@Getter
public enum AccountStatus {
    PENDING_CONTRACT("Chờ hợp đồng"),
    ACTIVE("Đang hoạt động"),
    DORMANT("Tạm ngưng do không có hợp đồng"),
    INACTIVE("Ngừng hoạt động"),
    REJECTED("Bị từ chối"),
    CLOSED("Đã đóng"),
    ARCHIVED("Đã lưu trữ");
    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }
}
