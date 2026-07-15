package com.sep490.hdbhms.billingandpayment.application.service;

import java.math.BigDecimal;

public class UtilityBillingRunServiceSelfCheck {
    public static void main(String[] args) {
        assert UtilityBillingRunService.billableQuantity(new BigDecimal("7.1"), 6L) == 2;
        assert UtilityBillingRunService.billableQuantity(new BigDecimal("6.0"), 6L) == 0;
        assert UtilityBillingRunService.billableQuantity(new BigDecimal("0"), 0L) == 0;
    }
}
