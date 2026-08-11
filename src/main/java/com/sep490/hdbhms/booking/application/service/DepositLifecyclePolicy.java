package com.sep490.hdbhms.booking.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.booking.domain.value_objects.DepositContactOutcome;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;

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
            throw new AppException(ApiErrorCode.DEPOSIT_EXTENSION_INVALID);
        }
        if (extensionCount >= maxExtensions) {
            throw new AppException(ApiErrorCode.DEPOSIT_EXTENSION_INVALID);
        }
        if (additionalDays < 1 || additionalDays > MAX_EXTENSION_DAYS) {
            throw new AppException(ApiErrorCode.DEPOSIT_EXTENSION_INVALID);
        }
        LocalDate nextDate = currentExpectedMoveInDate.plusDays(additionalDays);
        if (nextDate.isBefore(today)) {
            throw new AppException(ApiErrorCode.DEPOSIT_EXTENSION_INVALID);
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
