package com.sep490.hdbhms.portal.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.FloorStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyType;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.portal.application.port.in.query.GetDashboardQuery;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Proxy;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        RoomEntity vacant = room(201L, second, secondFloor, RoomStatus.VACANT);

        GetDashboardService service = service(
                method -> method.equals("findAllByDeletedAtIsNull") ? List.of(first, second) : List.of(),
                args -> ((Long) args[0]).equals(1L) ? List.of(firstFloor) : List.of(secondFloor),
                (method, args) -> {
                    if (method.equals("findAllByProperty_IdAndFloor_Id")) {
                        return ((Long) args[0]).equals(1L) ? List.of(occupied) : List.of(vacant);
                    }
                    if (method.equals("findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc")) {
                        return ((Long) args[0]).equals(1L) ? List.of(occupied) : List.of(vacant);
                    }
                    return List.of();
                },
                method -> List.of()
        );

        var response = service.execute(new GetDashboardQuery(7L, Role.OWNER));

        assertEquals(2, response.getTotalRoomCount());
        assertEquals(1, response.getTotalOccupiedRoomCount());
        assertEquals(1, response.getTotalVacantRoomCount());
        assertEquals(2, response.getFloorEfficiencies().size());
        assertEquals("Cơ sở A", response.getFloorEfficiencies().getFirst().getPropertyName());
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
        return new GetDashboardService(properties, floors, rooms, promotions, mock(JdbcTemplate.class));
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
}
