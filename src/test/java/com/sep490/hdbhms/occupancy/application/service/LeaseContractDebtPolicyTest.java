package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaseContractDebtPolicyTest {
    @Test
    void outstandingDebtBlocksLifecycleAction() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(47L)))
                .thenReturn(125000L);

        AppException exception = assertThrows(
                AppException.class,
                () -> LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, 47L)
        );

        assertEquals(ApiErrorCode.CONTRACT_OUTSTANDING_DEBT, exception.getApiErrorCode());
        assertEquals("Hợp đồng còn công nợ 125000 VNĐ. Vui lòng thanh toán hết công nợ trước khi thực hiện thao tác.",
                exception.getMessage());
    }

    @Test
    void paidContractHasNoOutstandingDebt() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(47L)))
                .thenReturn(0L);

        LeaseContractDebtPolicy.requireNoOutstandingDebt(jdbcTemplate, 47L);
    }
}
