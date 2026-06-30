package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaseContractLifecycleServiceTest {

    @Test
    void activeContractBecomesExpiringSoonAtThreeMonthBoundary() {
        LocalDate today = LocalDate.of(2026, 6, 6);

        LeaseStatus result = LeaseContractLifecycleService.resolveTargetStatus(
                LeaseStatus.ACTIVE,
                LocalDate.of(2026, 9, 6),
                today,
                false
        );

        assertEquals(LeaseStatus.EXPIRING_SOON, result);
    }

    @Test
    void expiringSoonContractBecomesExpiredAfterEndDate() {
        LeaseStatus result = LeaseContractLifecycleService.resolveTargetStatus(
                LeaseStatus.EXPIRING_SOON,
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 6),
                false
        );

        assertEquals(LeaseStatus.EXPIRED, result);
    }

    @Test
    void contractDoesNotExpireWhenRenewedContractExists() {
        LeaseStatus result = LeaseContractLifecycleService.resolveTargetStatus(
                LeaseStatus.EXPIRING_SOON,
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 6),
                true
        );

        assertEquals(LeaseStatus.EXPIRING_SOON, result);
    }

    @Test
    void contractExpiresWhenRenewalIsOnlyDraft() {
        LeaseStatus result = LeaseContractLifecycleService.resolveTargetStatus(
                LeaseStatus.EXPIRING_SOON,
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 6, 6),
                false
        );

        assertEquals(LeaseStatus.EXPIRED, result);
    }

    @Test
    void lifecycleIgnoresFinishedContractStatuses() {
        LeaseStatus result = LeaseContractLifecycleService.resolveTargetStatus(
                LeaseStatus.LIQUIDATED,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 6),
                false
        );

        assertEquals(LeaseStatus.LIQUIDATED, result);
    }
}
