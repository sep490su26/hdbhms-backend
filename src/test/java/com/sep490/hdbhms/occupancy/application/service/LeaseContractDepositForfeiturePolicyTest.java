package com.sep490.hdbhms.occupancy.application.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaseContractDepositForfeiturePolicyTest {

    @Test
    void forfeitsDepositWhenMoveOutIsLessThanOneMonthAway() {
        assertTrue(LeaseContractManagementService.isShortNoticeMoveOut(
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 1)
        ));
    }

    @Test
    void doesNotForfeitDepositWhenMoveOutIsExactlyOneMonthAway() {
        assertFalse(LeaseContractManagementService.isShortNoticeMoveOut(
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 7, 20)
        ));
    }

    @Test
    void doesNotForfeitDepositWhenNoticeIsAfterMoveOutDate() {
        assertFalse(LeaseContractManagementService.isShortNoticeMoveOut(
                LocalDate.of(2026, 8, 20),
                LocalDate.of(2026, 8, 21)
        ));
    }

    @Test
    void doesNotForfeitDepositWhenMoveOutDateIsMissing() {
        assertFalse(LeaseContractManagementService.isShortNoticeMoveOut(
                null,
                LocalDate.of(2026, 8, 1)
        ));
    }
}
