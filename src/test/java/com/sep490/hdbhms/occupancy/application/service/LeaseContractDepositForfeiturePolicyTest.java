package com.sep490.hdbhms.occupancy.application.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaseContractDepositForfeiturePolicyTest {

    @Test
    void forfeitsDepositWhenMoveOutIsBeforeEndDateWithLessThanOneMonthRemaining() {
        assertTrue(LeaseContractManagementService.isShortTermEarlyTermination(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 1)
        ));
    }

    @Test
    void doesNotForfeitDepositWhenExactlyOneMonthRemains() {
        assertFalse(LeaseContractManagementService.isShortTermEarlyTermination(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 7, 31)
        ));
    }

    @Test
    void doesNotForfeitDepositWhenMoveOutIsOnOrAfterContractEndDate() {
        assertFalse(LeaseContractManagementService.isShortTermEarlyTermination(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 8, 31)
        ));
        assertFalse(LeaseContractManagementService.isShortTermEarlyTermination(
                LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 9, 1)
        ));
    }
}
