package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.util.List;

public record MockUtilityInvoiceResponse(
        Long invoiceId,
        String invoiceCode,
        String billingPeriod,
        Long propertyId,
        Long roomId,
        String roomCode,
        Long contractId,
        String contractCode,
        String status,
        Long totalAmount,
        Boolean created,
        String message,
        List<Line> lines
) {
    public record Line(
            Long invoiceLineId,
            String lineType,
            String description,
            Integer quantity,
            Long unitPrice,
            Long amount,
            Long meterReadingId
    ) {
    }
}
