package com.sep490.hdbhms.accounting.application.service;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseStatus;
import com.sep490.hdbhms.accounting.infrastructure.persistence.entity.ExpenseApprovalRequestEntity;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.infrastructure.persistence.entity.ChangeRequestEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseRequestServiceTest {

    @Test
    void mapsLiquidationDepositRefundStatuses() {
        ExpenseApprovalRequestEntity pendingApproval = ExpenseApprovalRequestEntity.builder()
                .changeRequest(ChangeRequestEntity.builder().status(RequestStatus.PENDING).build())
                .build();

        assertEquals(
                "WAITING_OWNER_APPROVAL",
                ExpenseRequestService.liquidationRefundStatus(ExpenseStatus.PENDING_APPROVAL, pendingApproval, null)
        );
        assertEquals(
                "APPROVED_WAITING_TENANT_CONFIRMATION",
                ExpenseRequestService.liquidationRefundStatus(ExpenseStatus.READY_FOR_PAYMENT, pendingApproval, null)
        );
        assertEquals(
                "RECORDED_BY_MANAGER",
                ExpenseRequestService.liquidationRefundStatus(ExpenseStatus.PAID, pendingApproval, null)
        );
        assertEquals(
                "TENANT_CONFIRMED",
                ExpenseRequestService.liquidationRefundStatus(ExpenseStatus.PAID, pendingApproval, "TENANT_CONFIRMED")
        );
    }
}
