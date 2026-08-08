package com.sep490.hdbhms.property.application.service;

import java.math.BigDecimal;

public record MeterReadingAnomalySettings(
        BigDecimal highUsageMultiplier,
        BigDecimal highUsageMinDelta,
        BigDecimal highUsageAbsoluteLimit,
        boolean negativeUsageBlocking
) {
    public static MeterReadingAnomalySettings defaults() {
        return new MeterReadingAnomalySettings(
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(500),
                true
        );
    }
}
