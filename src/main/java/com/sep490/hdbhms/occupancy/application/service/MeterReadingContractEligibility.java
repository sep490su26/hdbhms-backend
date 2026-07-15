package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;

import java.util.List;

final class MeterReadingContractEligibility {
    static final List<LeaseStatus> STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING,
            LeaseStatus.EXPIRED,
            LeaseStatus.LIQUIDATED,
            LeaseStatus.RENEWED
    );

    private MeterReadingContractEligibility() {
    }
}
