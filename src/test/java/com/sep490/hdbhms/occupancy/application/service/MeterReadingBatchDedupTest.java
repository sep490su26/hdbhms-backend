package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.occupancy.domain.model.MeterReadingBatch;
import com.sep490.hdbhms.occupancy.domain.value_objects.BatchStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.MeterReadingBatchEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeterReadingBatchDedupTest {

    @Test
    void historyKeepsOneBatchPerPeriodAndPrefersActiveThenLatest() {
        MeterReadingBatchEntity oldConfirmed = batchEntity(10L, "2026-07", BatchStatus.CONFIRMED);
        MeterReadingBatchEntity draft = batchEntity(11L, "2026-07", BatchStatus.DRAFT);
        MeterReadingBatchEntity olderJune = batchEntity(8L, "2026-06", BatchStatus.CONFIRMED);
        MeterReadingBatchEntity latestJune = batchEntity(9L, "2026-06", BatchStatus.CONFIRMED);

        List<MeterReadingBatchEntity> rows = GetBatchMeterReadingsService.dedupeByPeriod(
                List.of(oldConfirmed, draft, olderJune, latestJune)
        );

        assertEquals(List.of(11L, 9L), rows.stream().map(MeterReadingBatchEntity::getId).toList());
    }

    @Test
    void startBatchReusesPreferredExistingBatch() {
        MeterReadingBatch oldConfirmed = batch(10L, "2026-07", BatchStatus.CONFIRMED);
        MeterReadingBatch draft = batch(11L, "2026-07", BatchStatus.DRAFT);

        assertEquals(
                11L,
                SubmitMeterReadingService.selectPreferredBatch(List.of(oldConfirmed, draft)).orElseThrow().getId()
        );
    }

    @Test
    void meterReadingEligibilityUsesRentGeneratingContractStatuses() {
        assertTrue(MeterReadingContractEligibility.STATUSES.contains(LeaseStatus.ACTIVE));
        assertTrue(MeterReadingContractEligibility.STATUSES.contains(LeaseStatus.EXPIRED));
        assertTrue(MeterReadingContractEligibility.STATUSES.contains(LeaseStatus.LIQUIDATED));

        assertFalse(MeterReadingContractEligibility.STATUSES.contains(LeaseStatus.DRAFT));
        assertFalse(MeterReadingContractEligibility.STATUSES.contains(LeaseStatus.SIGNED));
        assertFalse(MeterReadingContractEligibility.STATUSES.contains(LeaseStatus.TRANSFERRED));
    }

    private static MeterReadingBatchEntity batchEntity(Long id, String period, BatchStatus status) {
        MeterReadingBatchEntity batch = new MeterReadingBatchEntity();
        batch.setId(id);
        batch.setReadingPeriod(period);
        batch.setStatus(status);
        return batch;
    }

    private static MeterReadingBatch batch(Long id, String period, BatchStatus status) {
        return MeterReadingBatch.builder()
                .id(id)
                .readingPeriod(period)
                .status(status)
                .build();
    }
}
