package com.sep490.hdbhms.scheduling.application.handler;

import java.time.Duration;

public record ScheduledTaskPolicy(
        boolean singleInstance,
        Duration lockDuration,
        int maxAttempts,
        Duration retryDelay
) {
    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(30);
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofMinutes(5);
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    public static ScheduledTaskPolicy standard() {
        return new ScheduledTaskPolicy(false, DEFAULT_LOCK_DURATION, DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_DELAY);
    }

    public static ScheduledTaskPolicy singleInstancePolicy() {
        return new ScheduledTaskPolicy(true, DEFAULT_LOCK_DURATION, DEFAULT_MAX_ATTEMPTS, DEFAULT_RETRY_DELAY);
    }
}
