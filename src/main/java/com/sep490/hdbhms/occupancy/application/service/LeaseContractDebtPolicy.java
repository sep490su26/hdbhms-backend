package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Shared lifecycle guard for invoices that still have a payable balance. */
public final class LeaseContractDebtPolicy {
    private LeaseContractDebtPolicy() {
    }

    public static long outstandingAmount(JdbcTemplate jdbcTemplate, Long leaseContractId) {
        if (jdbcTemplate == null || leaseContractId == null) {
            return 0L;
        }
        Long amount = jdbcTemplate.queryForObject("""
                        SELECT COALESCE(SUM(remaining_amount), 0)
                        FROM invoices
                        WHERE lease_contract_id = ?
                          AND status IN ('ISSUED', 'PARTIALLY_PAID', 'OVERDUE')
                          AND COALESCE(remaining_amount, 0) > 0
                        """,
                Long.class,
                leaseContractId
        );
        return amount == null ? 0L : amount;
    }

    public static void requireNoOutstandingDebt(JdbcTemplate jdbcTemplate, Long leaseContractId) {
        long amount = outstandingAmount(jdbcTemplate, leaseContractId);
        if (amount > 0) {
            throw new AppException(ApiErrorCode.CONTRACT_OUTSTANDING_DEBT, amount);
        }
    }

    public static String blockingReason(long amount) {
        return amount > 0
                ? ApiErrorCode.CONTRACT_OUTSTANDING_DEBT.getDetails().formatted(amount)
                : null;
    }
}
