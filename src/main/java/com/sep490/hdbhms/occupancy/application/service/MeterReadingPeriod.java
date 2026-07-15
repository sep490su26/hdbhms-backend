package com.sep490.hdbhms.occupancy.application.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

final class MeterReadingPeriod {
    private static final DateTimeFormatter CANONICAL = DateTimeFormatter.ofPattern("MM-uuuu");
    private static final DateTimeFormatter MONTH_YEAR_DASH = DateTimeFormatter.ofPattern("M-uuuu");
    private static final DateTimeFormatter MONTH_YEAR_SLASH = DateTimeFormatter.ofPattern("M/uuuu");
    private static final DateTimeFormatter YEAR_MONTH_DASH = DateTimeFormatter.ofPattern("uuuu-M");

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
            return YearMonth.parse(period, MONTH_YEAR_SLASH).format(CANONICAL);
        }
        if (period.indexOf('-') == 4) {
            return YearMonth.parse(period, YEAR_MONTH_DASH).format(CANONICAL);
        }
        return YearMonth.parse(period, MONTH_YEAR_DASH).format(CANONICAL);
    }

    static YearMonth parse(String value) {
        return YearMonth.parse(normalize(value), CANONICAL);
    }
}
