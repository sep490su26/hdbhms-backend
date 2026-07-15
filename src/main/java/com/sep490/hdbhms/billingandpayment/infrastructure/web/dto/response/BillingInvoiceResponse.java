package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record BillingInvoiceResponse(
        Long id,
        String invoiceCode,
        String invoiceType,
        String invoiceReason,
        String billingPeriod,
        String status,
        Long propertyId,
        String propertyName,
        Long roomId,
        String roomCode,
        Long contractId,
        String contractCode,
        String tenantName,
        LocalDateTime issueDate,
        LocalDateTime dueDate,
        Long subtotalAmount,
        Long discountAmount,
        Long totalAmount,
        Long paidAmount,
        Long remainingAmount,
        List<BillingInvoiceLineResponse> lines,
        List<BillingPaymentHistoryResponse> paymentHistory
) {
}
