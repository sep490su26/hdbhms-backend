package com.sep490.hdbhms.property.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/** Calculates usage for cumulative registers, including an explicit or obvious rollover. */
@Service
public class MeterUsageCalculator {
    private static final BigDecimal ROLLOVER_HIGH_WATERMARK = BigDecimal.valueOf(0.90);
    private static final BigDecimal ROLLOVER_LOW_WATERMARK = BigDecimal.valueOf(0.10);

    public Calculation calculate(
            BigDecimal previousValue,
            BigDecimal currentValue,
            BigDecimal counterCapacity,
            Integer requestedRolloverCount
    ) {
        BigDecimal previous = safe(previousValue);
        BigDecimal current = safe(currentValue);
        BigDecimal capacity = safe(counterCapacity);
        int requested = requestedRolloverCount == null ? 0 : requestedRolloverCount;

        if (current.signum() < 0 || previous.signum() < 0 || capacity.signum() <= 0
                || previous.compareTo(capacity) >= 0 || current.compareTo(capacity) >= 0
                || requested < 0) {
            return Calculation.invalid();
        }

        int rolloverCount = requested;
        if (rolloverCount == 0 && current.compareTo(previous) < 0 && looksLikeRollover(previous, current, capacity)) {
            rolloverCount = 1;
        }

        if (current.compareTo(previous) < 0 && rolloverCount == 0) {
            return Calculation.invalid();
        }

        BigDecimal usage = current
                .subtract(previous)
                .add(capacity.multiply(BigDecimal.valueOf(rolloverCount)));
        if (usage.signum() < 0) {
            return Calculation.invalid();
        }
        return new Calculation(true, rolloverCount, capacity, usage);
    }

    private boolean looksLikeRollover(BigDecimal previous, BigDecimal current, BigDecimal capacity) {
        BigDecimal highWatermark = capacity.multiply(ROLLOVER_HIGH_WATERMARK).setScale(3, RoundingMode.HALF_UP);
        BigDecimal lowWatermark = capacity.multiply(ROLLOVER_LOW_WATERMARK).setScale(3, RoundingMode.HALF_UP);
        return previous.compareTo(highWatermark) >= 0 && current.compareTo(lowWatermark) <= 0;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public record Calculation(
            boolean valid,
            int rolloverCount,
            BigDecimal counterCapacity,
            BigDecimal usage
    ) {
        static Calculation invalid() {
            return new Calculation(false, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
