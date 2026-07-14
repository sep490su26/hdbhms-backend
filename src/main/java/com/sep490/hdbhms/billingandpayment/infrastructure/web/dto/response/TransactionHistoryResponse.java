package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record TransactionHistoryResponse(
        Long id,
        Long transactionId,
        String transactionCode,
        LocalDateTime transactionTime,
        Long roomId,
        String roomCode,
        String propertyName,
        String tenantName,
        Long amount,
        String paymentType,
        String invoiceType,
        String status,
        String provider,
        Long invoiceId,
        String invoiceCode,
        String payerName,
        String content,
        String billingPeriod,
        LocalDateTime invoiceIssueDate,
        LocalDateTime invoiceDueDate
) {
}
