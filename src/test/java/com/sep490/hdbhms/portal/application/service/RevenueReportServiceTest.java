package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.property.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.property.domain.value_objects.PropertyType;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevenueReportServiceTest {

    @Test
    void revenueReportQueriesPaidInvoicesWithoutPaymentAllocations() {
        JpaPropertyRepository properties = mock(JpaPropertyRepository.class);
        when(properties.findAllByDeletedAtIsNull()).thenReturn(List.of(property(1L)));
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();

        RevenueReportService service = new RevenueReportService(
                properties,
                mock(JpaRolePromotionRepository.class),
                jdbcTemplate
        );

        service.getRevenueReport(7L, Role.OWNER, "month", "2026-07");

        assertFalse(jdbcTemplate.sql().stream().anyMatch(sql -> sql.contains("payment_allocations")));
        assertFalse(jdbcTemplate.sql().stream().anyMatch(sql -> sql.contains("payment_transactions")));
        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql ->
                sql.contains("FROM invoices invoice")
                        && sql.contains("invoice.paid_amount *")
                        && sql.contains("invoice.invoice_type <> 'DEPOSIT'")
                        && sql.contains("invoice.updated_at >= ?")
        ));
    }

    private static PropertyEntity property(Long id) {
        return PropertyEntity.builder()
                .id(id)
                .propertyCode("P-" + id)
                .name("Property " + id)
                .propertyType(PropertyType.BOARDING_HOUSE)
                .addressLine("Address " + id)
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<String> sql = new ArrayList<>();

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql.add(sql);
            return List.of();
        }

        List<String> sql() {
            return sql;
        }
    }
}
