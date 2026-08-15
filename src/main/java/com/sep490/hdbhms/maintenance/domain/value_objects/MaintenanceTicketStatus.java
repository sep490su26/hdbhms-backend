package com.sep490.hdbhms.maintenance.domain.value_objects;

public enum MaintenanceTicketStatus {
    PENDING_ACCEPTANCE,
    ACCEPTED,
    WAITING_TENANT_DECISION,
    IN_PROGRESS,
    WAITING_CONFIRMATION,
    COMPLETED,
    REJECTED,
    CANCELLED
}
