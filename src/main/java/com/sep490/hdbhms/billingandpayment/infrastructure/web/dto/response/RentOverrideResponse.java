package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record RentOverrideResponse(
        Long id,
        Long roomId,
        String roomCode,
        Long contractId,
        String billingPeriod,
        Long oldMonthlyRent,
        Long overrideMonthlyRent,
        boolean invoiceApplied,
        Long invoiceId,
        String invoiceStatus,
        LocalDateTime createdAt
) {
}
