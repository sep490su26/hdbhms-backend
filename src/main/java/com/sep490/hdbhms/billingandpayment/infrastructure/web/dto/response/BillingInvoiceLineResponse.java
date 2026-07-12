package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

public record BillingInvoiceLineResponse(
        Long id,
        String lineType,
        String description,
        Integer quantity,
        Long unitPrice,
        Long amount
) {
}
