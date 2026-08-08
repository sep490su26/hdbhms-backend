package com.sep490.hdbhms.property.domain.policy;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReadingWindow {

    //TODO: Change these to 25, 16 is only for testing-purpose
    public static boolean isOpen(LocalDate date) {
        int day = date.getDayOfMonth();
        return day >= 5 || day <= 1;
    }

    public static LocalDate calculateNextOpenDate(LocalDate date) {
        int day = date.getDayOfMonth();
        if (day >= 5 || day <= 1) {
            return null;
        }
        return date.withDayOfMonth(10);
    }
}
