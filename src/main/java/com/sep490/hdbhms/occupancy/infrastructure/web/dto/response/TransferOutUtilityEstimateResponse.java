package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.math.BigDecimal;

public record TransferOutUtilityEstimateResponse(
        MeterChargeEstimate electricity,
        MeterChargeEstimate water,
        Long incidentalAmount,
        Long serviceFeeAmount,
        Long totalAmount
) {
    public record MeterChargeEstimate(
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal usage,
            Long freeAllowance,
            Integer billableQuantity,
            Long unitPrice,
            Long amount
    ) {}
}
