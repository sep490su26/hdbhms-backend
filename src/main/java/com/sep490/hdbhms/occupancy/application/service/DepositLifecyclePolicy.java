package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.DepositContactOutcome;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

public final class DepositLifecyclePolicy {
    public static final int MAX_EXTENSION_DAYS = 7;
    public static final int FORFEITURE_WAIT_DAYS = 14;
    private static final Set<DepositAgreementStatus> ACTIVE_STATUSES = EnumSet.of(
            DepositAgreementStatus.PAID,
            DepositAgreementStatus.CONFIRMED,
            DepositAgreementStatus.EXTENDED
    );

    private DepositLifecyclePolicy() {
    }

    public static boolean isActive(DepositAgreementStatus status) {
        return ACTIVE_STATUSES.contains(status);
    }

    public static LocalDate calculateExtensionDate(
            DepositAgreementStatus status,
            LocalDate currentExpectedMoveInDate,
            int extensionCount,
            int maxExtensions,
            int additionalDays,
            LocalDate today
    ) {
        if (!isActive(status)) {
            throw new IllegalStateException("Chỉ được gia hạn khoản cọc đang giữ chỗ.");
        }
        if (extensionCount >= maxExtensions) {
            throw new IllegalStateException("Khoản cọc này đã sử dụng hết số lần gia hạn.");
        }
        if (additionalDays < 1 || additionalDays > MAX_EXTENSION_DAYS) {
            throw new IllegalArgumentException("Chỉ được gia hạn từ 1 đến 7 ngày.");
        }
        LocalDate nextDate = currentExpectedMoveInDate.plusDays(additionalDays);
        if (nextDate.isBefore(today)) {
            throw new IllegalStateException("Ngày gia hạn mới đã ở trong quá khứ.");
        }
        return nextDate;
    }

    public static LocalDate forfeitureDecisionDate(LocalDate expectedMoveInDate) {
        return expectedMoveInDate.plusDays(FORFEITURE_WAIT_DAYS);
    }

    public static long overdueDays(
            DepositAgreementStatus status,
            LocalDate expectedMoveInDate,
            LocalDate today
    ) {
        if (!isActive(status) || !today.isAfter(expectedMoveInDate)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(expectedMoveInDate, today);
    }

    public static boolean isContactRequired(
            DepositAgreementStatus status,
            LocalDate expectedMoveInDate,
            LocalDate today,
            LocalDateTime lastContactedAt
    ) {
        return isActive(status)
                && !today.isBefore(expectedMoveInDate)
                && (lastContactedAt == null || lastContactedAt.toLocalDate().isBefore(expectedMoveInDate));
    }

    public static boolean isForfeitureEligible(
            DepositAgreementStatus status,
            LocalDate expectedMoveInDate,
            LocalDate today,
            DepositContactOutcome latestOutcome,
            LocalDateTime lastContactedAt
    ) {
        return isActive(status)
                && !today.isBefore(forfeitureDecisionDate(expectedMoveInDate))
                && latestOutcome == DepositContactOutcome.UNREACHABLE
                && lastContactedAt != null
                && !lastContactedAt.toLocalDate().isBefore(expectedMoveInDate);
    }
}
