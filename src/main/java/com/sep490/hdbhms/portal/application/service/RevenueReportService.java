package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.RevenueReportResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RevenueReportService {
    static final int PERIOD_COUNT = 6;

    JpaPropertyRepository propertyRepository;
    JpaRolePromotionRepository rolePromotionRepository;
    JdbcTemplate jdbcTemplate;

    public RevenueReportResponse getRevenueReport(Long userId, Role role, String periodType, String endPeriod) {
        ReportPeriodType type = ReportPeriodType.from(periodType);
        YearMonth end = parseEndPeriod(endPeriod);
        List<Long> propertyIds = scopedPropertyIds(userId, role);
        List<PeriodWindow> windows = type.windows(end, PERIOD_COUNT);

        if (propertyIds.isEmpty()) {
            return emptyReport(type, end, windows);
        }

        YearMonth queryStart = windows.getFirst().start().minusYears(1);
        YearMonth queryEndExclusive = type.plusOne(windows.getLast().start());
        Map<String, RevenueBucket> buckets = revenueBuckets(type, propertyIds, queryStart, queryEndExclusive);

        List<RevenueReportResponse.RevenuePeriodResponse> periods = windows.stream()
                .map(window -> toPeriodResponse(window, buckets))
                .toList();
        RevenueReportResponse.RevenuePeriodResponse selected = periods.getLast();
        long previousTotalRevenue = safe(selected.previous());

        return new RevenueReportResponse(
                type.responseValue(),
                end.toString(),
                safe(selected.total()),
                previousTotalRevenue,
                growth(safe(selected.total()), previousTotalRevenue),
                periods,
                sources(selected)
        );
    }

    private List<Long> scopedPropertyIds(Long userId, Role role) {
        List<PropertyEntity> properties;
        if (role == Role.MANAGER) {
            List<Long> assignedIds = rolePromotionRepository.findActivePropertyIds(
                            userId,
                            PromotionRole.MANAGER,
                            RolePromotionStatus.ACTIVE
                    ).stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            properties = assignedIds.isEmpty()
                    ? List.of()
                    : propertyRepository.findAllByIdInAndDeletedAtIsNull(assignedIds);
        } else {
            properties = propertyRepository.findAllByDeletedAtIsNull();
        }

        return properties.stream()
                .sorted(Comparator.comparing(PropertyEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(PropertyEntity::getId)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, RevenueBucket> revenueBuckets(
            ReportPeriodType type,
            List<Long> propertyIds,
            YearMonth start,
            YearMonth endExclusive
    ) {
        String propertyClause = inClause("invoice.property_id", propertyIds.size());
        List<Object> params = new ArrayList<>(propertyIds);
        params.add(start.atDay(1).atStartOfDay());
        params.add(endExclusive.atDay(1).atStartOfDay());

        String sql = """
                SELECT bucket.period,
                       COALESCE(SUM(CASE WHEN bucket.category = 'room' THEN bucket.allocated_amount ELSE 0 END), 0) AS room,
                       COALESCE(SUM(CASE WHEN bucket.category = 'utilities' THEN bucket.allocated_amount ELSE 0 END), 0) AS utilities,
                       COALESCE(SUM(CASE WHEN bucket.category = 'service' THEN bucket.allocated_amount ELSE 0 END), 0) AS service,
                       COALESCE(SUM(CASE WHEN bucket.category = 'extra' THEN bucket.allocated_amount ELSE 0 END), 0) AS extra
                FROM (
                    SELECT %s AS period,
                           CASE
                               WHEN line.line_type = 'ROOM_RENT' THEN 'room'
                               WHEN line.line_type IN ('ELECTRICITY', 'WATER') THEN 'utilities'
                               WHEN line.line_type = 'SERVICE_FEE' THEN 'service'
                               WHEN line.invoice_line_id IS NULL AND invoice.invoice_type = 'RENT' THEN 'room'
                               WHEN line.invoice_line_id IS NULL AND invoice.invoice_type = 'UTILITY' THEN 'utilities'
                               ELSE 'extra'
                           END AS category,
                           CASE
                               WHEN line.invoice_line_id IS NULL OR invoice.total_amount <= 0 THEN allocation.amount
                               ELSE allocation.amount * COALESCE(line.amount, line.unit_price * COALESCE(line.quantity, 1), 0) / invoice.total_amount
                           END AS allocated_amount
                    FROM payment_allocations allocation
                    JOIN payment_transactions payment
                      ON payment.payment_transaction_id = allocation.payment_transaction_id
                    JOIN invoices invoice
                      ON invoice.invoice_id = allocation.invoice_id
                    LEFT JOIN invoice_lines line
                      ON line.invoice_id = invoice.invoice_id
                    WHERE %s
                      AND payment.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED')
                      AND payment.transaction_time >= ?
                      AND payment.transaction_time < ?
                ) bucket
                GROUP BY bucket.period
                """.formatted(type.sqlPeriodExpression(), propertyClause);

        List<RevenueBucket> rows = jdbcTemplate.query(sql, this::mapBucket, params.toArray());
        Map<String, RevenueBucket> result = new LinkedHashMap<>();
        if (rows != null) {
            rows.forEach(row -> result.put(row.period(), row));
        }
        return result;
    }

    private RevenueReportResponse.RevenuePeriodResponse toPeriodResponse(
            PeriodWindow window,
            Map<String, RevenueBucket> buckets
    ) {
        RevenueBucket current = buckets.getOrDefault(window.key(), RevenueBucket.empty(window.key()));
        RevenueBucket previous = buckets.getOrDefault(window.previousKey(), RevenueBucket.empty(window.previousKey()));
        return new RevenueReportResponse.RevenuePeriodResponse(
                window.key(),
                window.label(),
                current.room(),
                current.utilities(),
                current.service(),
                current.extra(),
                current.total(),
                previous.total()
        );
    }

    private RevenueBucket mapBucket(ResultSet rs, int rowNumber) throws SQLException {
        return new RevenueBucket(
                rs.getString("period"),
                readRoundedLong(rs, "room"),
                readRoundedLong(rs, "utilities"),
                readRoundedLong(rs, "service"),
                readRoundedLong(rs, "extra")
        );
    }

    private long readRoundedLong(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? 0L : value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private RevenueReportResponse emptyReport(
            ReportPeriodType type,
            YearMonth end,
            List<PeriodWindow> windows
    ) {
        List<RevenueReportResponse.RevenuePeriodResponse> periods = windows.stream()
                .map(window -> new RevenueReportResponse.RevenuePeriodResponse(
                        window.key(),
                        window.label(),
                        0L,
                        0L,
                        0L,
                        0L,
                        0L,
                        0L
                ))
                .toList();

        return new RevenueReportResponse(
                type.responseValue(),
                end.toString(),
                0L,
                0L,
                0.0,
                periods,
                sources(periods.getLast())
        );
    }

    private List<RevenueReportResponse.RevenueSourceResponse> sources(
            RevenueReportResponse.RevenuePeriodResponse period
    ) {
        long total = Math.max(safe(period.total()), 0L);
        return List.of(
                source("room", period.room(), total),
                source("utilities", period.utilities(), total),
                source("service", period.service(), total),
                source("extra", period.extra(), total)
        );
    }

    private RevenueReportResponse.RevenueSourceResponse source(String key, Long amount, long total) {
        long safeAmount = safe(amount);
        int percent = total == 0L ? 0 : (int) Math.round((safeAmount * 100.0) / total);
        return new RevenueReportResponse.RevenueSourceResponse(key, safeAmount, percent);
    }

    private YearMonth parseEndPeriod(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endPeriod must use yyyy-MM format");
        }
    }

    private double growth(long current, long previous) {
        if (previous == 0L) {
            return current > 0L ? 100.0 : 0.0;
        }
        return Math.round(((current - previous) * 1000.0) / previous) / 10.0;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private String inClause(String columnName, int size) {
        return columnName + " IN (" + String.join(", ", Collections.nCopies(size, "?")) + ")";
    }

    enum ReportPeriodType {
        MONTH("month") {
            @Override
            String sqlPeriodExpression() {
                return "DATE_FORMAT(payment.transaction_time, '%Y-%m')";
            }

            @Override
            YearMonth start(YearMonth end, int offset) {
                return end.minusMonths(offset);
            }

            @Override
            YearMonth plusOne(YearMonth start) {
                return start.plusMonths(1);
            }

            @Override
            String key(YearMonth start) {
                return start.toString();
            }

            @Override
            String label(YearMonth start) {
                return "T" + start.getMonthValue();
            }
        },
        QUARTER("quarter") {
            @Override
            String sqlPeriodExpression() {
                return "CONCAT(YEAR(payment.transaction_time), '-Q', QUARTER(payment.transaction_time))";
            }

            @Override
            YearMonth start(YearMonth end, int offset) {
                YearMonth quarterStart = YearMonth.of(end.getYear(), ((end.getMonthValue() - 1) / 3) * 3 + 1);
                return quarterStart.minusMonths(offset * 3L);
            }

            @Override
            YearMonth plusOne(YearMonth start) {
                return start.plusMonths(3);
            }

            @Override
            String key(YearMonth start) {
                return start.getYear() + "-Q" + (((start.getMonthValue() - 1) / 3) + 1);
            }

            @Override
            String label(YearMonth start) {
                return "Q" + (((start.getMonthValue() - 1) / 3) + 1) + "/" + start.getYear();
            }
        },
        YEAR("year") {
            @Override
            String sqlPeriodExpression() {
                return "CAST(YEAR(payment.transaction_time) AS CHAR)";
            }

            @Override
            YearMonth start(YearMonth end, int offset) {
                return YearMonth.of(end.getYear() - offset, 1);
            }

            @Override
            YearMonth plusOne(YearMonth start) {
                return start.plusYears(1);
            }

            @Override
            String key(YearMonth start) {
                return String.valueOf(start.getYear());
            }

            @Override
            String label(YearMonth start) {
                return String.valueOf(start.getYear());
            }
        };

        private final String responseValue;

        ReportPeriodType(String responseValue) {
            this.responseValue = responseValue;
        }

        static ReportPeriodType from(String value) {
            if (value == null || value.isBlank()) {
                return MONTH;
            }
            try {
                return ReportPeriodType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "periodType must be month, quarter, or year");
            }
        }

        List<PeriodWindow> windows(YearMonth end, int count) {
            List<PeriodWindow> result = new ArrayList<>();
            for (int index = count - 1; index >= 0; index--) {
                YearMonth start = start(end, index);
                result.add(new PeriodWindow(
                        start,
                        key(start),
                        key(start.minusYears(1)),
                        label(start)
                ));
            }
            return result;
        }

        String responseValue() {
            return responseValue;
        }

        abstract String sqlPeriodExpression();

        abstract YearMonth start(YearMonth end, int offset);

        abstract YearMonth plusOne(YearMonth start);

        abstract String key(YearMonth start);

        abstract String label(YearMonth start);
    }

    private record PeriodWindow(YearMonth start, String key, String previousKey, String label) {
    }

    private record RevenueBucket(String period, long room, long utilities, long service, long extra) {
        static RevenueBucket empty(String period) {
            return new RevenueBucket(period, 0L, 0L, 0L, 0L);
        }

        long total() {
            return room + utilities + service + extra;
        }
    }
}
