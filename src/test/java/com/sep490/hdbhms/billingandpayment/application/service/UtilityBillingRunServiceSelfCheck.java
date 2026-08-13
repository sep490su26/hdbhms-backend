package com.sep490.hdbhms.billingandpayment.application.service;

import java.math.BigDecimal;
import java.time.YearMonth;

public class UtilityBillingRunServiceSelfCheck {
    public static void main(String[] args) {
        assert UtilityBillingRunService.billableQuantity(new BigDecimal("7.1"), 6L) == 2;
        assert UtilityBillingRunService.billableQuantity(new BigDecimal("6.0"), 6L) == 0;
        assert UtilityBillingRunService.billableQuantity(new BigDecimal("0"), 0L) == 0;
        assert UtilityBillingRunService.isServiceFeeDue(YearMonth.of(2026, 1), YearMonth.of(2026, 1), 3);
        assert !UtilityBillingRunService.isServiceFeeDue(YearMonth.of(2026, 2), YearMonth.of(2026, 1), 3);
        assert UtilityBillingRunService.isServiceFeeDue(YearMonth.of(2026, 4), YearMonth.of(2026, 1), 3);
    }
}
