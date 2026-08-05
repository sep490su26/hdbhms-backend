package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;

import java.math.BigDecimal;

public record LiquidationChargeInput(
        InvoiceLineType lineType,
        String description,
        Integer quantity,
        Long unitPrice,
        BigDecimal previousValue,
        BigDecimal currentValue,
        Long photoFileId
) {
}
