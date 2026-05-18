package com.sep490.hdbhms.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstantUtils {
    public static boolean isFixedUnitsAgoFromNow(Instant instant, long amount, ChronoUnit unit) {
        return instant.isAfter(Instant.now().minus(amount, unit));
    }
}
