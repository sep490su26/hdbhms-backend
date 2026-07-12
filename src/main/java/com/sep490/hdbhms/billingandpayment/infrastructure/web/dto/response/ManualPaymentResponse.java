package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

public record ManualPaymentResponse(
        BillingInvoiceResponse invoice,
        BillingPaymentHistoryResponse payment
) {
}
