package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.util.List;

public record MockUtilityInvoiceBatchResponse(
        Long propertyId,
        String billingPeriod,
        Integer createdCount,
        Integer skippedCount,
        List<MockUtilityInvoiceResponse> results
) {
}
