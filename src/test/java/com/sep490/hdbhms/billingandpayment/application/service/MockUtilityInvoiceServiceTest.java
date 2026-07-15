package com.sep490.hdbhms.billingandpayment.application.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockUtilityInvoiceServiceTest {

    @Test
    void billableQuantitySubtractsAllowanceAndRoundsUp() {
        assertEquals(0, MockUtilityInvoiceService.billableQuantity(new BigDecimal("5.5"), 6));
        assertEquals(1, MockUtilityInvoiceService.billableQuantity(new BigDecimal("6.1"), 6));
        assertEquals(3, MockUtilityInvoiceService.billableQuantity(new BigDecimal("8.01"), 6));
    }
}
