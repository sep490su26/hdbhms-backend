package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record UtilityBillingRunResponse(
        Long runId,
        Long propertyId,
        String propertyName,
        String billingPeriod,
        String invoiceReason,
        String status,
        Integer totalRooms,
        Integer readyCount,
        Integer warningCount,
        Integer skippedCount,
        Integer generatedInvoiceCount,
        Long subtotalAmount,
        Long discountAmount,
        Long totalAmount,
        LocalDateTime generatedAt,
        List<Item> items
) {
    public record Item(
            Long itemId,
            Long roomId,
            String roomCode,
            Long contractId,
            String contractCode,
            Long electricityReadingId,
            BigDecimal electricityPrevious,
            BigDecimal electricityCurrent,
            BigDecimal electricityUsage,
            Integer electricityQuantity,
            Long electricityUnitPrice,
            Long electricityAmount,
            Long waterReadingId,
            BigDecimal waterPrevious,
            BigDecimal waterCurrent,
            BigDecimal waterUsage,
            Integer waterQuantity,
            Long waterUnitPrice,
            Long waterAmount,
            Long serviceFeeUnitPrice,
            Long serviceFeeAmount,
            Boolean serviceFeeWaived,
            String serviceFeeWaiveReason,
            Long subtotalAmount,
            Long discountAmount,
            Long totalAmount,
            String warningMessage,
            String adjustmentReason,
            String status,
            Long invoiceId,
            String invoiceCode
    ) {
    }
}
