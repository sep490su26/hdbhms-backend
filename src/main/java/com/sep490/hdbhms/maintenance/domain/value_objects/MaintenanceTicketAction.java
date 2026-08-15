package com.sep490.hdbhms.maintenance.domain.value_objects;

public enum MaintenanceTicketAction {
    CREATE,
    ACCEPT,
    REJECT,
    SEND_REPAIR_PROPOSAL,
    TENANT_APPROVE_REPAIR,
    TENANT_REJECT_REPAIR,
    START_PROGRESS,
    UPDATE_REPAIR_INFO,
    ATTACH_FILE,
    REQUEST_CONFIRMATION,
    CONFIRM_COMPLETED,
    REPORT_NOT_FIXED,
    REVIEW
}
