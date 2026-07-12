package com.sep490.hdbhms.occupancy.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

final class MeterReadingPeriod {
    private static final DateTimeFormatter CANONICAL = DateTimeFormatter.ofPattern("uuuu-MM");
    private static final DateTimeFormatter LEGACY = DateTimeFormatter.ofPattern("M/uuuu");

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
            return YearMonth.parse(period, LEGACY).format(CANONICAL);
        }
        return YearMonth.parse(period, CANONICAL).format(CANONICAL);
    }

    static YearMonth parse(String value) {
        return YearMonth.parse(normalize(value), CANONICAL);
    }
}
