package com.sep490.hdbhms.occupancy.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReadingWindow {

    //TODO: Change these to 25, 16 is only for testing-purpose
    public static boolean isOpen(LocalDate date) {
        int day = date.getDayOfMonth();
        return day >= 10 || day <= 5;
    }

    public static LocalDate calculateNextOpenDate(LocalDate date) {
        int day = date.getDayOfMonth();
        if (day >= 10 || day <= 5) {
            return null;
        }
        return date.withDayOfMonth(10);
    }
}
