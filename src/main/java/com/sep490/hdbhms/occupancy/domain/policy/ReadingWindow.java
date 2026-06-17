package com.sep490.hdbhms.occupancy.domain.policy;

import java.time.LocalDate;

public class ReadingWindow {

    public static boolean isOpen(LocalDate date) {
        int day = date.getDayOfMonth();
        return day >= 16 || day <= 10;
    }

    public static LocalDate calculateNextOpenDate(LocalDate date) {
        int day = date.getDayOfMonth();
        if (day >= 16 || day <= 10) {
            return null;
        }
        return date.withDayOfMonth(16);
    }
}
