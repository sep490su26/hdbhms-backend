package com.sep490.hdbhms.billingandpayment.application.service;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.UtilityBillingRunItemStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilityBillingRunServiceTest {

    @Test
    void warningTakesPriorityOverSkippedWhenReadingCannotBeInvoiced() {
        assertEquals(
                UtilityBillingRunItemStatus.WARNING,
                UtilityBillingRunService.resolveItemStatus(true, false, 0)
        );
    }

    @Test
    void zeroSubtotalWithoutWarningRemainsSkipped() {
        assertEquals(
                UtilityBillingRunItemStatus.SKIPPED,
                UtilityBillingRunService.resolveItemStatus(false, true, 0)
        );
    }

    @Test
    void positiveSubtotalWithoutWarningIsReady() {
        assertEquals(
                UtilityBillingRunItemStatus.READY,
                UtilityBillingRunService.resolveItemStatus(false, true, 1)
        );
    }

    @Test
    void serviceFeeIsWaivedOnlyBelowTheElectricityThreshold() {
        assertEquals(
                true,
                UtilityBillingRunService.isServiceFeeWaived(99999L, 100000L)
        );
        assertEquals(
                false,
                UtilityBillingRunService.isServiceFeeWaived(100000L, 100000L)
        );
        assertEquals(
                false,
                UtilityBillingRunService.isServiceFeeWaived(50000L, null)
        );
    }
}
