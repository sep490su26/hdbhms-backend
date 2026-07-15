package com.sep490.hdbhms.occupancy.application.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MeterReadingPeriodTest {

    @Test
    void keepsCanonicalMonthYear() {
        assertEquals("07-2026", MeterReadingPeriod.normalize("07-2026"));
    }

    @Test
    void convertsLegacyFormatsToCanonicalFormat() {
        assertEquals("07-2026", MeterReadingPeriod.normalize("2026-07"));
        assertEquals("07-2026", MeterReadingPeriod.normalize("07/2026"));
        assertEquals("07-2026", MeterReadingPeriod.normalize("7-2026"));
    }

    @Test
    void createsCanonicalPeriodFromDate() {
        assertEquals("07-2026", MeterReadingPeriod.from(LocalDate.of(2026, 7, 13)));
    }

    @Test
    void parsesBothSupportedFormats() {
        assertEquals(YearMonth.of(2026, 7), MeterReadingPeriod.parse("07-2026"));
        assertEquals(YearMonth.of(2026, 7), MeterReadingPeriod.parse("2026-07"));
        assertEquals(YearMonth.of(2026, 7), MeterReadingPeriod.parse("7/2026"));
    }
}
