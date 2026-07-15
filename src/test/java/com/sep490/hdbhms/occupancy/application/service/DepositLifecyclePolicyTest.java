package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.DepositAgreementStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.DepositContactOutcome;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DepositLifecyclePolicyTest {

    @Test
    void extensionAllowsOneToSevenDaysOnlyOnce() {
        LocalDate dueDate = LocalDate.of(2026, 7, 13);

        assertEquals(
                LocalDate.of(2026, 7, 20),
                DepositLifecyclePolicy.calculateExtensionDate(
                        DepositAgreementStatus.PAID, dueDate, 0, 1, 7, dueDate
                )
        );
        assertThrows(IllegalArgumentException.class, () ->
                DepositLifecyclePolicy.calculateExtensionDate(
                        DepositAgreementStatus.PAID, dueDate, 0, 1, 8, dueDate
                ));
        assertThrows(IllegalStateException.class, () ->
                DepositLifecyclePolicy.calculateExtensionDate(
                        DepositAgreementStatus.EXTENDED, dueDate, 1, 1, 1, dueDate
                ));
    }

    @Test
    void extensionRejectsAResultThatIsAlreadyInThePast() {
        assertThrows(IllegalStateException.class, () ->
                DepositLifecyclePolicy.calculateExtensionDate(
                        DepositAgreementStatus.PAID,
                        LocalDate.of(2026, 7, 1),
                        0,
                        1,
                        7,
                        LocalDate.of(2026, 7, 13)
                ));
    }

    @Test
    void forfeitureRequiresFourteenDaysAndLatestUnreachableContactAfterDueDate() {
        LocalDate dueDate = LocalDate.of(2026, 7, 1);
        LocalDateTime contactAtDueDate = dueDate.atTime(9, 0);

        assertFalse(DepositLifecyclePolicy.isForfeitureEligible(
                DepositAgreementStatus.PAID,
                dueDate,
                dueDate.plusDays(13),
                DepositContactOutcome.UNREACHABLE,
                contactAtDueDate
        ));
        assertFalse(DepositLifecyclePolicy.isForfeitureEligible(
                DepositAgreementStatus.PAID,
                dueDate,
                dueDate.plusDays(14),
                DepositContactOutcome.REACHED,
                contactAtDueDate
        ));
        assertFalse(DepositLifecyclePolicy.isForfeitureEligible(
                DepositAgreementStatus.PAID,
                dueDate,
                dueDate.plusDays(14),
                DepositContactOutcome.UNREACHABLE,
                dueDate.minusDays(1).atTime(9, 0)
        ));
        assertTrue(DepositLifecyclePolicy.isForfeitureEligible(
                DepositAgreementStatus.PAID,
                dueDate,
                dueDate.plusDays(14),
                DepositContactOutcome.UNREACHABLE,
                contactAtDueDate
        ));
    }

    @Test
    void contactBecomesRequiredOnDueDateAndAgainAfterAnExtension() {
        LocalDate dueDate = LocalDate.of(2026, 7, 13);

        assertFalse(DepositLifecyclePolicy.isContactRequired(
                DepositAgreementStatus.PAID, dueDate, dueDate.minusDays(1), null
        ));
        assertTrue(DepositLifecyclePolicy.isContactRequired(
                DepositAgreementStatus.PAID, dueDate, dueDate, null
        ));
        assertFalse(DepositLifecyclePolicy.isContactRequired(
                DepositAgreementStatus.PAID, dueDate, dueDate, dueDate.atTime(8, 0)
        ));
        assertTrue(DepositLifecyclePolicy.isContactRequired(
                DepositAgreementStatus.EXTENDED,
                dueDate.plusDays(7),
                dueDate.plusDays(7),
                dueDate.atTime(8, 0)
        ));
    }

    @Test
    void overdueDaysOnlyApplyToActiveDeposits() {
        LocalDate dueDate = LocalDate.of(2026, 7, 1);
        LocalDate today = LocalDate.of(2026, 7, 13);

        assertEquals(12, DepositLifecyclePolicy.overdueDays(
                DepositAgreementStatus.PAID, dueDate, today
        ));
        assertEquals(12, DepositLifecyclePolicy.overdueDays(
                DepositAgreementStatus.EXTENDED, dueDate, today
        ));
        assertEquals(0, DepositLifecyclePolicy.overdueDays(
                DepositAgreementStatus.CONVERTED_TO_LEASE, dueDate, today
        ));
        assertEquals(0, DepositLifecyclePolicy.overdueDays(
                DepositAgreementStatus.REFUNDED, dueDate, today
        ));
        assertEquals(0, DepositLifecyclePolicy.overdueDays(
                DepositAgreementStatus.FORFEITED, dueDate, today
        ));
    }
}
