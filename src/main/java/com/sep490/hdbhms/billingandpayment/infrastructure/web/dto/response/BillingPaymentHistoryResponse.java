package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record BillingPaymentHistoryResponse(
        Long id,
        Long transactionId,
        Long amount,
        String provider,
        String status,
        String payerName,
        String content,
        Long confirmedBy,
        LocalDateTime confirmedAt,
        Long allocatedBy,
        LocalDateTime allocatedAt
) {
}
