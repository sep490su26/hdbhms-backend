package com.sep490.hdbhms.property.application.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterUsageCalculatorTest {
    private final MeterUsageCalculator calculator = new MeterUsageCalculator();
    private final BigDecimal capacity = BigDecimal.valueOf(100000);

    @Test
    void calculatesNormalUsage() {
        var result = calculator.calculate(value("100"), value("130"), capacity, null);

        assertTrue(result.valid());
        assertEquals(0, result.rolloverCount());
        assertEquals(value("30"), result.usage());
    }

    @Test
    void detectsObviousRollover() {
        var result = calculator.calculate(value("99998"), value("3"), capacity, null);

        assertTrue(result.valid());
        assertEquals(1, result.rolloverCount());
        assertEquals(value("5"), result.usage());
    }

    @Test
    void calculatesExplicitRollover() {
        var result = calculator.calculate(value("500"), value("5"), capacity, 1);

        assertTrue(result.valid());
        assertEquals(value("99505"), result.usage());
    }

    @Test
    void calculatesExplicitRolloverWhenDisplayValueIsHigherAfterMissedCycles() {
        var result = calculator.calculate(value("500"), value("600"), capacity, 1);

        assertTrue(result.valid());
        assertEquals(value("100100"), result.usage());
    }

    @Test
    void firstReadingStartsFromZero() {
        var result = calculator.calculate(BigDecimal.ZERO, value("500"), capacity, null);

        assertTrue(result.valid());
        assertEquals(value("500"), result.usage());
    }

    @Test
    void autoDetectionUsesTheConfiguredBoundary() {
        var result = calculator.calculate(value("90000"), value("10000"), capacity, null);

        assertTrue(result.valid());
        assertEquals(1, result.rolloverCount());
        assertEquals(value("20000"), result.usage());
    }

    @Test
    void rejectsUnconfirmedDecrease() {
        var result = calculator.calculate(value("500"), value("5"), capacity, null);

        assertFalse(result.valid());
    }

    @Test
    void rejectsValueOutsideRegisterCapacity() {
        var result = calculator.calculate(value("99999"), value("100000"), capacity, null);

        assertFalse(result.valid());
    }

    private BigDecimal value(String value) {
        return new BigDecimal(value);
    }
}
