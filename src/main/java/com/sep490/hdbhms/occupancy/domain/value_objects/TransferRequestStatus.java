package com.sep490.hdbhms.occupancy.domain.value_objects;

public enum TransferRequestStatus {
    WAITING_APPROVAL,
    CANCELLED,
    REJECTED,
    WAITING_NEW_CONTRACT,
    WAITING_TARGET_HOLDER_APPROVAL,

    WAITING_CONTRACT_CONFIRMATION,
    WAITING_SIGNING,
    WAITING_EXECUTION,
    EXECUTED,
    EXPIRED
}
