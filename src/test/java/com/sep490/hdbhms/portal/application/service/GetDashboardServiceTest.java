package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.property.domain.value_objects.FloorStatus;
import com.sep490.hdbhms.property.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.property.domain.value_objects.PropertyType;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.portal.application.port.in.query.GetDashboardQuery;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GetDashboardServiceTest {

    @Test
    void dashboardUtilityUsageUsesMeterReadingPeriodFormat() {
        assertEquals("06-2026", YearMonth.of(2026, 6).format(GetDashboardService.METER_PERIOD_FORMAT));
    }

    @Test
    void ownerSummaryIncludesAllScopedPropertiesWithoutFixedPropertyId() {
        PropertyEntity first = property(1L, "Cơ sở A");
        PropertyEntity second = property(2L, "Cơ sở B");
        FloorEntity firstFloor = floor(11L, first, "Tầng 1");
        FloorEntity secondFloor = floor(21L, second, "Tầng 2");
        RoomEntity occupied = room(101L, first, firstFloor, RoomStatus.OCCUPIED);
        RoomEntity soonVacant = room(102L, first, firstFloor, RoomStatus.SOON_VACANT);
        RoomEntity vacant = room(201L, second, secondFloor, RoomStatus.VACANT);
        RoomEntity expired = room(202L, second, secondFloor, RoomStatus.EXPIRED);

        GetDashboardService service = service(
                method -> method.equals("findAllByDeletedAtIsNull") ? List.of(first, second) : List.of(),
                args -> ((Long) args[0]).equals(1L) ? List.of(firstFloor) : List.of(secondFloor),
                (method, args) -> {
                    if (method.equals("findAllByProperty_IdAndFloor_Id")) {
                        return ((Long) args[0]).equals(1L) ? List.of(occupied, soonVacant) : List.of(vacant, expired);
                    }
                    if (method.equals("findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc")) {
                        return ((Long) args[0]).equals(1L) ? List.of(occupied, soonVacant) : List.of(vacant, expired);
                    }
                    return List.of();
                },
                method -> List.of()
        );

        var response = service.execute(new GetDashboardQuery(7L, Role.OWNER));

        assertEquals(4, response.getTotalRoomCount());
        assertEquals(2, response.getTotalOccupiedRoomCount());
        assertEquals(1, response.getTotalVacantRoomCount());
        assertEquals(2, response.getFloorEfficiencies().size());
        assertEquals("Cơ sở A", response.getFloorEfficiencies().getFirst().getPropertyName());
    }

    @Test
    void dashboardQueriesConfirmedPaymentsByTransactionTime() {
        PropertyEntity property = property(1L, "A");
        FloorEntity floor = floor(11L, property, "F1");
        RoomEntity room = room(101L, property, floor, RoomStatus.OCCUPIED);
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();

        GetDashboardService service = service(
                method -> method.equals("findAllByDeletedAtIsNull") ? List.of(property) : List.of(),
                args -> List.of(floor),
                (method, args) -> List.of(room),
                method -> List.of(),
                jdbcTemplate
        );

        service.execute(new GetDashboardQuery(7L, Role.OWNER));

        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql -> sql.contains("payment_allocations")));
        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql -> sql.contains("payment_transactions")));
        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql ->
                sql.contains("FROM invoices invoice")
                        && sql.contains("SUM(allocation.amount)")
                        && sql.contains("invoice.invoice_type <> 'DEPOSIT'")
                        && sql.contains("payment.transaction_time >= ?")
        ));
        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql ->
                sql.contains("FROM visit_requests visit")
                        && sql.contains("visit.status = 'NOT_VIEWED'")
        ));
        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql ->
                sql.contains("FROM maintenance_tickets ticket")
                        && sql.contains("ticket.status = 'PENDING_ACCEPTANCE'")
        ));
        assertTrue(jdbcTemplate.sql().stream().anyMatch(sql ->
                sql.contains("FROM change_requests change_request")
                        && sql.contains("change_request.status = 'PENDING'")
        ));
        assertTrue(jdbcTemplate.sql().stream().filter(sql -> sql.contains("FROM invoices invoice"))
                .count() >= 3);
    }

    @Test
    void managerWithNoAssignmentsReceivesEmptyDashboard() {
        GetDashboardService service = service(
                method -> List.of(),
                args -> List.of(),
                (method, args) -> List.of(),
                method -> List.of()
        );

        var response = service.execute(new GetDashboardQuery(9L, Role.MANAGER));

        assertEquals(0, response.getTotalRoomCount());
        assertTrue(response.getFloorEfficiencies().isEmpty());
    }

    private GetDashboardService service(
            MethodResult propertyResults,
            ArgumentsResult floorResults,
            NamedArgumentsResult roomResults,
            MethodResult promotionResults
    ) {
        return service(propertyResults, floorResults, roomResults, promotionResults, mock(JdbcTemplate.class));
    }

    private GetDashboardService service(
            MethodResult propertyResults,
            ArgumentsResult floorResults,
            NamedArgumentsResult roomResults,
            MethodResult promotionResults,
            JdbcTemplate jdbcTemplate
    ) {
        JpaPropertyRepository properties = proxy(
                JpaPropertyRepository.class,
                (method, args) -> propertyResults.get(method)
        );
        JpaFloorRepository floors = proxy(
                JpaFloorRepository.class,
                (method, args) -> floorResults.get(args)
        );
        JpaRoomRepository rooms = proxy(JpaRoomRepository.class, roomResults::get);
        JpaRolePromotionRepository promotions = proxy(
                JpaRolePromotionRepository.class,
                (method, args) -> promotionResults.get(method)
        );
        return new GetDashboardService(properties, floors, rooms, promotions, jdbcTemplate);
    }

    private <T> T proxy(Class<T> type, InvocationResult results) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return type.getSimpleName() + "Stub";
                    }
                    return results.get(method.getName(), args == null ? new Object[0] : args);
                }
        ));
    }

    private PropertyEntity property(Long id, String name) {
        return PropertyEntity.builder()
                .id(id)
                .propertyCode("CS-" + id)
                .name(name)
                .propertyType(PropertyType.BOARDING_HOUSE)
                .addressLine("Địa chỉ " + id)
                .status(PropertyStatus.ACTIVE)
                .build();
    }

    private FloorEntity floor(Long id, PropertyEntity property, String name) {
        return FloorEntity.builder()
                .id(id)
                .property(property)
                .floorCode("F-" + id)
                .name(name)
                .sortOrder(id.intValue())
                .status(FloorStatus.ACTIVE)
                .build();
    }

    private RoomEntity room(
            Long id,
            PropertyEntity property,
            FloorEntity floor,
            RoomStatus status
    ) {
        return RoomEntity.builder()
                .id(id)
                .property(property)
                .floor(floor)
                .roomCode("P" + id)
                .name("Phòng " + id)
                .currentStatus(status)
                .sortOrder(id.intValue())
                .build();
    }

    @FunctionalInterface
    private interface InvocationResult {
        Object get(String methodName, Object[] args);
    }

    @FunctionalInterface
    private interface MethodResult {
        Object get(String methodName);
    }

    @FunctionalInterface
    private interface ArgumentsResult {
        Object get(Object[] args);
    }

    @FunctionalInterface
    private interface NamedArgumentsResult {
        Object get(String methodName, Object[] args);
    }

    private static class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<String> sql = new ArrayList<>();

        @Override
        public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
            this.sql.add(sql);
            return List.of();
        }

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
            this.sql.add(sql);
            if (Long.class.equals(requiredType)) {
                return requiredType.cast(0L);
            }
            return null;
        }

        List<String> sql() {
            return sql;
        }
    }
}
