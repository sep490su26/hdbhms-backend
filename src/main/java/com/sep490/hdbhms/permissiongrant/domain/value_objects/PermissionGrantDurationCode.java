package com.sep490.hdbhms.permissiongrant.domain.value_objects;

import java.time.Duration;
import java.util.Locale;

public enum PermissionGrantDurationCode {
    HOURS_48(Duration.ofHours(48)),
    DAYS_7(Duration.ofDays(7)),
    DAYS_30(Duration.ofDays(30));

    public static final PermissionGrantDurationCode DEFAULT = DAYS_30;

    private final Duration duration;

    PermissionGrantDurationCode(Duration duration) {
        this.duration = duration;
    }

    public Duration duration() {
        return duration;
    }

    public static PermissionGrantDurationCode fromNullable(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return PermissionGrantDurationCode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
