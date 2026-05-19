package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String toVietnameseDateString(LocalDate date) {
        return String.format(
                "ngày %s, tháng %s, năm %s",
                date.getDayOfMonth(),
                date.getMonthValue(),
                date.getYear()
        );
    }

    public static String toddMMyyyyDateString(LocalDate date) {
        return date.format(DD_MM_YYYY);
    }
}
