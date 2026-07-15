package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

public record DebtSummaryResponse(
        Long propertyId,
        String propertyName,
        Long roomId,
        String roomName,
        String tenantName,
        Long rentDebtAmount,
        Long utilityDebtAmount,
        Long totalDebt,
        Integer monthsOverdue,
        String debtType,
        Boolean isWarning
) {
}
