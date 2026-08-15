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
}
