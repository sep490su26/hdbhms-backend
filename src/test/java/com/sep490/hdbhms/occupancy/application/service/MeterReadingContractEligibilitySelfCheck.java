package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;

public class MeterReadingContractEligibilitySelfCheck {
    public static void main(String[] args) {
        assertEligible(LeaseStatus.ACTIVE);
        assertEligible(LeaseStatus.EXPIRING_SOON);
        assertEligible(LeaseStatus.TERMINATION_PENDING);
        assertNotEligible(LeaseStatus.EXPIRED);
        assertNotEligible(LeaseStatus.LIQUIDATED);
        assertNotEligible(LeaseStatus.RENEWED);
    }

    private static void assertEligible(LeaseStatus status) {
        if (!MeterReadingContractEligibility.STATUSES.contains(status)) {
            throw new AssertionError(status + " must require monthly meter reading");
        }
    }

    private static void assertNotEligible(LeaseStatus status) {
        if (MeterReadingContractEligibility.STATUSES.contains(status)) {
            throw new AssertionError(status + " must not require monthly meter reading");
        }
    }
}
