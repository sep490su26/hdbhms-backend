package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.portal.application.port.in.query.GetDashboardQuery;
import com.sep490.hdbhms.portal.application.port.in.usecase.GetDashboardUseCase;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.DashboardResponse;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.FloorEfficiencyResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetDashboardService implements GetDashboardUseCase {
    static final int REVENUE_MONTH_COUNT = 6;
    static final DateTimeFormatter REVENUE_PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    static final DateTimeFormatter METER_PERIOD_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");

    JpaPropertyRepository propertyRepository;
    JpaFloorRepository floorRepository;
    JpaRoomRepository roomRepository;
    JpaRolePromotionRepository rolePromotionRepository;
    JdbcTemplate jdbcTemplate;

    @Override
    public DashboardResponse execute(GetDashboardQuery query) {
        List<PropertyEntity> properties = scopedProperties(query).stream()
                .sorted(Comparator.comparing(PropertyEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<FloorEfficiencyResponse> floorEfficiencies = properties.stream()
                .flatMap(property -> floorRepository.findAllByProperty_Id(property.getId()).stream()
                        .filter(floor -> floor.getDeletedAt() == null)
                        .sorted(Comparator.comparing(FloorEntity::getSortOrder)
                                .thenComparing(FloorEntity::getName, String.CASE_INSENSITIVE_ORDER))
                        .map(floor -> toFloorEfficiency(property, floor)))
                .toList();

        List<RoomEntity> rooms = properties.stream()
                .flatMap(property -> roomRepository
                        .findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc(property.getId())
                        .stream())
                .toList();
        List<Long> propertyIds = properties.stream()
                .map(PropertyEntity::getId)
                .filter(Objects::nonNull)
                .toList();
        List<DashboardResponse.RevenuePointResponse> revenueSeries = revenueSeries(propertyIds);
        long currentMonthRevenue = revenueAmount(revenueSeries, YearMonth.now());
        long previousMonthRevenue = revenueAmount(revenueSeries, YearMonth.now().minusMonths(1));

        return DashboardResponse.builder()
                .totalRoomCount((long) rooms.size())
                .totalOccupiedRoomCount(countRooms(rooms, RoomStatus.OCCUPIED))
                .totalVacantRoomCount(countRooms(rooms, RoomStatus.VACANT))
                .floorEfficiencies(floorEfficiencies)
                .currentMonthRevenue(currentMonthRevenue)
                .previousMonthRevenue(previousMonthRevenue)
                .revenueGrowthPercent(revenueGrowth(currentMonthRevenue, previousMonthRevenue))
                .revenueSeries(revenueSeries)
                .totalDebtAmount(totalDebtAmount(propertyIds))
                .debtWarningRoomCount(debtWarningRoomCount(propertyIds))
                .utilityUsage(utilityUsage(propertyIds))
                .expiringContractSummary(expiringContractSummary(propertyIds))
                .recentActivities(recentActivities(propertyIds))
                .build();
    }

    private List<PropertyEntity> scopedProperties(GetDashboardQuery query) {
        if (query.role() != Role.MANAGER) {
            return propertyRepository.findAllByDeletedAtIsNull();
        }

        List<Long> propertyIds = rolePromotionRepository.findActivePropertyIds(
                        query.userId(),
                        PromotionRole.MANAGER,
                        RolePromotionStatus.ACTIVE
                ).stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return propertyIds.isEmpty()
                ? List.of()
                : propertyRepository.findAllByIdInAndDeletedAtIsNull(propertyIds);
    }

    private FloorEfficiencyResponse toFloorEfficiency(PropertyEntity property, FloorEntity floor) {
        List<RoomEntity> rooms = roomRepository.findAllByProperty_IdAndFloor_Id(
                        property.getId(),
                        floor.getId()
                ).stream()
                .filter(room -> room.getDeletedAt() == null)
                .toList();

        return FloorEfficiencyResponse.builder()
                .propertyId(property.getId())
                .propertyName(property.getName())
                .floorId(floor.getId())
                .floorName(floor.getName())
                .roomCount((long) rooms.size())
                .vacantRoomCount(countRooms(rooms, RoomStatus.VACANT))
                .build();
    }

    private long countRooms(List<RoomEntity> rooms, RoomStatus status) {
        return rooms.stream()
                .map(RoomEntity::getCurrentStatus)
                .filter(status::equals)
                .count();
    }

    private List<DashboardResponse.RevenuePointResponse> revenueSeries(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return emptyRevenueSeries();
        }

        YearMonth start = YearMonth.now().minusMonths(REVENUE_MONTH_COUNT - 1L);
        YearMonth endExclusive = YearMonth.now().plusMonths(1);
        String propertyClause = inClause("invoice.property_id", propertyIds.size());
        List<Object> params = new ArrayList<>(propertyIds);
        params.add(start.atDay(1).atStartOfDay());
        params.add(endExclusive.atDay(1).atStartOfDay());

        String sql = """
                SELECT DATE_FORMAT(payment.transaction_time, '%%Y-%%m') AS period,
                       COALESCE(SUM(allocation.amount), 0) AS amount
                FROM payment_allocations allocation
                JOIN payment_transactions payment
                  ON payment.payment_transaction_id = allocation.payment_transaction_id
                JOIN invoices invoice
                  ON invoice.invoice_id = allocation.invoice_id
                WHERE %s
                  AND payment.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED')
                  AND payment.transaction_time >= ?
                  AND payment.transaction_time < ?
                GROUP BY DATE_FORMAT(payment.transaction_time, '%%Y-%%m')
                """.formatted(propertyClause);

        List<RevenueBucket> buckets = jdbcTemplate.query(sql, this::mapRevenueBucket, params.toArray());
        Map<String, Long> amounts = new HashMap<>();
        if (buckets != null) {
            buckets.forEach(bucket -> amounts.put(bucket.period(), bucket.amount()));
        }

        long peak = Math.max(1L, amounts.values().stream().mapToLong(Long::longValue).max().orElse(0L));
        List<DashboardResponse.RevenuePointResponse> result = new ArrayList<>();
        for (int index = 0; index < REVENUE_MONTH_COUNT; index++) {
            YearMonth month = start.plusMonths(index);
            String period = month.format(REVENUE_PERIOD_FORMAT);
            long amount = amounts.getOrDefault(period, 0L);
            result.add(DashboardResponse.RevenuePointResponse.builder()
                    .period(period)
                    .label("T" + month.getMonthValue())
                    .amount(amount)
                    .percentOfPeak((int) Math.round((amount * 100.0) / peak))
                    .build());
        }
        return result;
    }

    private List<DashboardResponse.RevenuePointResponse> emptyRevenueSeries() {
        YearMonth start = YearMonth.now().minusMonths(REVENUE_MONTH_COUNT - 1L);
        List<DashboardResponse.RevenuePointResponse> result = new ArrayList<>();
        for (int index = 0; index < REVENUE_MONTH_COUNT; index++) {
            YearMonth month = start.plusMonths(index);
            result.add(DashboardResponse.RevenuePointResponse.builder()
                    .period(month.format(REVENUE_PERIOD_FORMAT))
                    .label("T" + month.getMonthValue())
                    .amount(0L)
                    .percentOfPeak(0)
                    .build());
        }
        return result;
    }

    private RevenueBucket mapRevenueBucket(ResultSet rs, int rowNumber) throws SQLException {
        return new RevenueBucket(rs.getString("period"), rs.getLong("amount"));
    }

    private long revenueAmount(List<DashboardResponse.RevenuePointResponse> revenueSeries, YearMonth month) {
        String period = month.format(REVENUE_PERIOD_FORMAT);
        return revenueSeries.stream()
                .filter(item -> period.equals(item.getPeriod()))
                .map(DashboardResponse.RevenuePointResponse::getAmount)
                .findFirst()
                .orElse(0L);
    }

    private double revenueGrowth(long currentMonthRevenue, long previousMonthRevenue) {
        if (previousMonthRevenue == 0L) {
            return currentMonthRevenue > 0L ? 100.0 : 0.0;
        }
        return Math.round(((currentMonthRevenue - previousMonthRevenue) * 1000.0) / previousMonthRevenue) / 10.0;
    }

    private long totalDebtAmount(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return 0L;
        }

        List<Object> params = new ArrayList<>(propertyIds);
        Long value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(remaining_amount), 0)
                FROM invoices
                WHERE %s
                  AND status IN ('OVERDUE', 'PARTIALLY_PAID')
                  AND invoice_type IN ('RENT', 'UTILITY')
                  AND remaining_amount > 0
                """.formatted(inClause("property_id", propertyIds.size())), Long.class, params.toArray());
        return value == null ? 0L : value;
    }

    private long debtWarningRoomCount(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return 0L;
        }

        List<Object> params = new ArrayList<>(propertyIds);
        Long value = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT room_id)
                FROM invoices
                WHERE %s
                  AND status = 'OVERDUE'
                  AND invoice_type IN ('RENT', 'UTILITY')
                  AND remaining_amount > 0
                """.formatted(inClause("property_id", propertyIds.size())), Long.class, params.toArray());
        return value == null ? 0L : value;
    }

    private DashboardResponse.UtilityUsageResponse utilityUsage(List<Long> propertyIds) {
        String period = YearMonth.now().format(METER_PERIOD_FORMAT);
        if (propertyIds.isEmpty()) {
            return DashboardResponse.UtilityUsageResponse.builder()
                    .period(period)
                    .electricityUsage(0.0)
                    .waterUsage(0.0)
                    .build();
        }

        List<Object> params = new ArrayList<>(propertyIds);
        params.add(period);
        String sql = """
                SELECT meter.meter_type AS meter_type,
                       COALESCE(SUM(reading.usage_amount), 0) AS total_usage
                FROM meter_readings reading
                JOIN meters meter
                  ON meter.meter_id = reading.meter_id
                JOIN rooms room
                  ON room.room_id = reading.room_id
                WHERE %s
                  AND reading.reading_period = ?
                  AND reading.status <> 'VOIDED'
                GROUP BY meter.meter_type
                """.formatted(inClause("room.property_id", propertyIds.size()));

        List<UtilityBucket> buckets = jdbcTemplate.query(sql, this::mapUtilityBucket, params.toArray());
        double electricity = 0.0;
        double water = 0.0;
        if (buckets != null) {
            for (UtilityBucket bucket : buckets) {
                if ("ELECTRICITY".equals(bucket.meterType())) {
                    electricity = bucket.usage();
                } else if ("WATER".equals(bucket.meterType())) {
                    water = bucket.usage();
                }
            }
        }

        return DashboardResponse.UtilityUsageResponse.builder()
                .period(period)
                .electricityUsage(electricity)
                .waterUsage(water)
                .build();
    }

    private UtilityBucket mapUtilityBucket(ResultSet rs, int rowNumber) throws SQLException {
        BigDecimal usage = rs.getBigDecimal("total_usage");
        return new UtilityBucket(
                rs.getString("meter_type"),
                usage == null ? 0.0 : usage.doubleValue()
        );
    }

    private DashboardResponse.ExpiringContractSummaryResponse expiringContractSummary(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return DashboardResponse.ExpiringContractSummaryResponse.builder()
                    .count(0L)
                    .tenants(List.of())
                    .build();
        }

        String where = """
                room.property_id %s
                  AND contract.deleted_at IS NULL
                  AND contract.status IN ('ACTIVE', 'EXPIRING_SOON')
                  AND contract.end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)
                """.formatted(inClauseOperator(propertyIds.size()));
        List<Object> countParams = new ArrayList<>(propertyIds);
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM lease_contracts contract
                JOIN rooms room
                  ON room.room_id = contract.room_id
                WHERE %s
                """.formatted(where), Long.class, countParams.toArray());

        List<Object> tenantParams = new ArrayList<>(propertyIds);
        List<DashboardResponse.ExpiringTenantResponse> tenants = jdbcTemplate.query("""
                SELECT profile.full_name AS full_name,
                       COALESCE(NULLIF(room.room_code, ''), room.name) AS room_name,
                       CAST(contract.end_date AS CHAR) AS end_date
                FROM lease_contracts contract
                JOIN rooms room
                  ON room.room_id = contract.room_id
                JOIN person_profiles profile
                  ON profile.person_profile_id = contract.primary_tenant_profile_id
                WHERE %s
                ORDER BY contract.end_date ASC, contract.lease_contract_id ASC
                LIMIT 4
                """.formatted(where), this::mapExpiringTenant, tenantParams.toArray());

        return DashboardResponse.ExpiringContractSummaryResponse.builder()
                .count(count == null ? 0L : count)
                .tenants(tenants == null ? List.of() : tenants)
                .build();
    }

    private DashboardResponse.ExpiringTenantResponse mapExpiringTenant(ResultSet rs, int rowNumber) throws SQLException {
        String fullName = rs.getString("full_name");
        return DashboardResponse.ExpiringTenantResponse.builder()
                .fullName(fullName)
                .initials(initials(fullName))
                .roomName(rs.getString("room_name"))
                .endDate(rs.getString("end_date"))
                .build();
    }

    private List<DashboardResponse.RecentActivityResponse> recentActivities(List<Long> propertyIds) {
        if (propertyIds.isEmpty()) {
            return List.of();
        }

        List<ActivityRow> rows = new ArrayList<>();
        rows.addAll(paymentActivities(propertyIds));
        rows.addAll(contractActivities(propertyIds));
        rows.addAll(maintenanceActivities(propertyIds));

        return rows.stream()
                .sorted(Comparator.comparing(ActivityRow::occurredAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::toRecentActivity)
                .toList();
    }

    private List<ActivityRow> paymentActivities(List<Long> propertyIds) {
        List<Object> params = new ArrayList<>(propertyIds);
        List<ActivityRow> rows = jdbcTemplate.query("""
                SELECT 'PAYMENT' AS type,
                       CONCAT('Thanh toán thành công: Phòng ', COALESCE(NULLIF(room.room_code, ''), room.name, invoice.invoice_code)) AS title,
                       'success' AS tone,
                       payment.transaction_time AS occurred_at
                FROM payment_allocations allocation
                JOIN payment_transactions payment
                  ON payment.payment_transaction_id = allocation.payment_transaction_id
                JOIN invoices invoice
                  ON invoice.invoice_id = allocation.invoice_id
                LEFT JOIN rooms room
                  ON room.room_id = invoice.room_id
                WHERE %s
                  AND payment.status IN ('MATCHED', 'PARTIALLY_ALLOCATED', 'ALLOCATED')
                ORDER BY payment.transaction_time DESC
                LIMIT 5
                """.formatted(inClause("invoice.property_id", propertyIds.size())), this::mapActivity, params.toArray());
        return rows == null ? List.of() : rows;
    }

    private List<ActivityRow> contractActivities(List<Long> propertyIds) {
        List<Object> params = new ArrayList<>(propertyIds);
        List<ActivityRow> rows = jdbcTemplate.query("""
                SELECT 'TENANT' AS type,
                       CONCAT('Người thuê mới: ', COALESCE(profile.full_name, 'Khách thuê'), ' đăng ký ', COALESCE(NULLIF(room.room_code, ''), room.name)) AS title,
                       'info' AS tone,
                       contract.created_at AS occurred_at
                FROM lease_contracts contract
                JOIN rooms room
                  ON room.room_id = contract.room_id
                JOIN person_profiles profile
                  ON profile.person_profile_id = contract.primary_tenant_profile_id
                WHERE %s
                  AND contract.deleted_at IS NULL
                ORDER BY contract.created_at DESC
                LIMIT 5
                """.formatted(inClause("room.property_id", propertyIds.size())), this::mapActivity, params.toArray());
        return rows == null ? List.of() : rows;
    }

    private List<ActivityRow> maintenanceActivities(List<Long> propertyIds) {
        List<Object> params = new ArrayList<>(propertyIds);
        List<ActivityRow> rows = jdbcTemplate.query("""
                SELECT 'MAINTENANCE' AS type,
                       CONCAT('Yêu cầu sửa chữa: ', COALESCE(NULLIF(room.room_code, ''), room.name, property.name), ' - ', ticket.title) AS title,
                       'warning' AS tone,
                       ticket.created_at AS occurred_at
                FROM maintenance_tickets ticket
                JOIN properties property
                  ON property.property_id = ticket.property_id
                LEFT JOIN rooms room
                  ON room.room_id = ticket.room_id
                WHERE %s
                ORDER BY ticket.created_at DESC
                LIMIT 5
                """.formatted(inClause("ticket.property_id", propertyIds.size())), this::mapActivity, params.toArray());
        return rows == null ? List.of() : rows;
    }

    private ActivityRow mapActivity(ResultSet rs, int rowNumber) throws SQLException {
        return new ActivityRow(
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("tone"),
                readLocalDateTime(rs, "occurred_at")
        );
    }

    private DashboardResponse.RecentActivityResponse toRecentActivity(ActivityRow row) {
        return DashboardResponse.RecentActivityResponse.builder()
                .type(row.type())
                .title(row.title())
                .tone(row.tone())
                .time(relativeTime(row.occurredAt()))
                .occurredAt(row.occurredAt() == null ? null : row.occurredAt().toString())
                .build();
    }

    private String inClause(String columnName, int size) {
        return columnName + " " + inClauseOperator(size);
    }

    private String inClauseOperator(int size) {
        return "IN (" + String.join(", ", Collections.nCopies(size, "?")) + ")";
    }

    private LocalDateTime readLocalDateTime(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof Instant instant) {
            return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        }
        return null;
    }

    private String relativeTime(LocalDateTime occurredAt) {
        if (occurredAt == null) {
            return "";
        }
        Duration duration = Duration.between(occurredAt, LocalDateTime.now());
        if (duration.toMinutes() < 1) {
            return "Vừa xong";
        }
        if (duration.toHours() < 1) {
            return duration.toMinutes() + " phút trước";
        }
        if (duration.toDays() < 1) {
            return duration.toHours() + " giờ trước";
        }
        if (duration.toDays() < 30) {
            return duration.toDays() + " ngày trước";
        }
        return occurredAt.toLocalDate().toString();
    }

    private String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] words = fullName.trim().split("\\s+");
        String first = words[0].substring(0, 1);
        String second = words.length > 1 ? words[words.length - 1].substring(0, 1) : "";
        return (first + second).toUpperCase(Locale.ROOT);
    }

    private record RevenueBucket(String period, long amount) {
    }

    private record UtilityBucket(String meterType, double usage) {
    }

    private record ActivityRow(String type, String title, String tone, LocalDateTime occurredAt) {
    }
}
