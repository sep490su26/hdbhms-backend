package com.sep490.hdbhms.occupancy.domain.value_objects;

public enum VisitRequestStatus {
    PENDING("Chờ xem"),
    VIEWED("Đã xem"),
    CANCELLED("Hủy hẹn");

    private final String label;

    VisitRequestStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
