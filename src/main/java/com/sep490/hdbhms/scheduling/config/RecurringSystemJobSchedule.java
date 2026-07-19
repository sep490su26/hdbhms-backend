package com.sep490.hdbhms.scheduling.config;

import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;

final class RecurringSystemJobSchedule {
    static final String TARGET_TYPE = "SYSTEM_JOB";
    static final long TARGET_ID = 0L;

    private static final Map<TaskType, String> EXPRESSIONS = new EnumMap<>(TaskType.class);

    static {
        EXPRESSIONS.put(TaskType.UTILITY_MONTHLY_RUN, "MONTHLY:25@02:00");
        EXPRESSIONS.put(TaskType.SCHEDULED_BILLING_CHARGES, "MONTHLY:1@08:00");
        EXPRESSIONS.put(TaskType.DEBT_OVERDUE_SCAN, "DAILY:01:00");
        EXPRESSIONS.put(TaskType.INVOICE_OVERDUE_WARNINGS, "DAILY:08:30");
        EXPRESSIONS.put(TaskType.NOTIFICATION_OUTBOX_DISPATCH, "FIXED_DELAY:PT60S");
        EXPRESSIONS.put(TaskType.EXPIRED_ROOM_HOLD_RECONCILE, "FIXED_DELAY:PT60S");
        EXPRESSIONS.put(TaskType.VISIT_REQUEST_TRASH_CLEANUP, "DAILY:03:00");
        EXPRESSIONS.put(TaskType.ROOM_TRANSFER_TIMEOUT, "HOURLY:00");
        EXPRESSIONS.put(TaskType.CONTRACT_LIFECYCLE_SCAN, "DAILY:00:05");
    }

    private RecurringSystemJobSchedule() {
    }

    static Map<TaskType, String> expressions() {
        return EXPRESSIONS;
    }

    static boolean isRecurring(TaskType taskType) {
        return EXPRESSIONS.containsKey(taskType);
    }

    static String expression(TaskType taskType) {
        return EXPRESSIONS.get(taskType);
    }

    static String idempotencyKey(TaskType taskType) {
        return TARGET_TYPE + ":" + taskType;
    }

    static LocalDateTime nextDueAt(TaskType taskType, LocalDateTime after) {
        return switch (taskType) {
            case UTILITY_MONTHLY_RUN -> nextMonthly(after, 25, LocalTime.of(2, 0));
            case SCHEDULED_BILLING_CHARGES -> nextMonthly(after, 1, LocalTime.of(8, 0));
            case DEBT_OVERDUE_SCAN -> nextDaily(after, LocalTime.of(1, 0));
            case INVOICE_OVERDUE_WARNINGS -> nextDaily(after, LocalTime.of(8, 30));
            case NOTIFICATION_OUTBOX_DISPATCH, EXPIRED_ROOM_HOLD_RECONCILE -> after.plusSeconds(60);
            case VISIT_REQUEST_TRASH_CLEANUP -> nextDaily(after, LocalTime.of(3, 0));
            case ROOM_TRANSFER_TIMEOUT -> after.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            case CONTRACT_LIFECYCLE_SCAN -> nextDaily(after, LocalTime.of(0, 5));
            default -> throw new IllegalArgumentException("Task type is not a recurring system job: " + taskType);
        };
    }

    private static LocalDateTime nextDaily(LocalDateTime after, LocalTime time) {
        LocalDateTime candidate = after.toLocalDate().atTime(time);
        return after.isBefore(candidate) ? candidate : candidate.plusDays(1);
    }

    private static LocalDateTime nextMonthly(LocalDateTime after, int dayOfMonth, LocalTime time) {
        YearMonth month = YearMonth.from(after);
        LocalDateTime candidate = month.atDay(dayOfMonth).atTime(time);
        return after.isBefore(candidate)
                ? candidate
                : month.plusMonths(1).atDay(dayOfMonth).atTime(time);
    }
}
