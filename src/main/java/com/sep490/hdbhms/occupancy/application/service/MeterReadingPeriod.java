package com.sep490.hdbhms.occupancy.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

final class MeterReadingPeriod {
    private static final DateTimeFormatter CANONICAL = DateTimeFormatter.ofPattern("MM-uuuu");
    private static final DateTimeFormatter LEGACY_ISO = DateTimeFormatter.ofPattern("uuuu-M");
    private static final DateTimeFormatter LEGACY_SLASH = DateTimeFormatter.ofPattern("M/uuuu");
    private static final DateTimeFormatter MONTH_YEAR = DateTimeFormatter.ofPattern("M-uuuu");

    private MeterReadingPeriod() {
    }

    static String current() {
        return YearMonth.now().format(CANONICAL);
    }

    static String from(LocalDate date) {
        return YearMonth.from(date).format(CANONICAL);
    }

    static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return current();
        }

        String period = value.trim();
        if (period.contains("/")) {
            return YearMonth.parse(period, LEGACY_SLASH).format(CANONICAL);
        }
        if (period.matches("\\d{4}-\\d{1,2}")) {
            return YearMonth.parse(period, LEGACY_ISO).format(CANONICAL);
        }
        return YearMonth.parse(period, MONTH_YEAR).format(CANONICAL);
    }

    static YearMonth parse(String value) {
        return YearMonth.parse(normalize(value), CANONICAL);
    }
}
