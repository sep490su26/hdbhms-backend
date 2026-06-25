package com.sep490.hdbhms.occupancy.application.service;

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
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetFacilitiesDashboardServiceTest {

    @Test
    void ownerDashboardBuildsSummaryAndFloorTreeFromDatabaseRows() {
        PropertyEntity property = property(1L, "HD-1", "Nhà trọ Hải Đăng", PropertyStatus.ACTIVE);
        FloorEntity floor = floor(10L, property, "F1", "Tầng 1");
        List<RoomEntity> rooms = List.of(
                room(100L, property, floor, "P101", RoomStatus.OCCUPIED),
                room(101L, property, floor, "P102", RoomStatus.VACANT),
                room(102L, property, floor, "P103", RoomStatus.MAINTENANCE)
        );

        GetFacilitiesDashboardService service = service(
                method -> method.equals("findAllByDeletedAtIsNull") ? List.of(property) : List.of(),
                method -> method.equals("findAllByProperty_Id") ? List.of(floor) : List.of(),
                method -> method.equals("findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc")
                        ? rooms
                        : List.of(),
                method -> List.of()
        );

        var response = service.getDashboard(7L, Role.OWNER, "", null);

        assertEquals(1, response.getSummary().getTotalProperties());
        assertEquals(1, response.getSummary().getActiveProperties());
        assertEquals(1, response.getSummary().getTotalFloors());
        assertEquals(3, response.getSummary().getTotalRooms());
        assertEquals(1, response.getSummary().getOccupiedRooms());
        assertEquals(1, response.getSummary().getVacantRooms());
        assertEquals(33, response.getSummary().getVacancyRate());
        assertEquals("Nhà trọ Hải Đăng", response.getFacilities().getFirst().getName());
        assertEquals(3, response.getFacilities().getFirst().getFloors().getFirst().getRooms().size());
    }

    @Test
    void managerOnlyReadsAssignedPropertiesAndHandlesZeroRooms() {
        PropertyEntity assigned = property(2L, "HD-2", "Cơ sở Riverside", PropertyStatus.TEMP_CLOSED);
        GetFacilitiesDashboardService service = service(
                method -> method.equals("findAllByIdInAndDeletedAtIsNull") ? List.of(assigned) : List.of(),
                method -> List.of(),
                method -> List.of(),
                method -> method.equals("findActivePropertyIds") ? List.of(2L) : List.of()
        );

        var response = service.getDashboard(9L, Role.MANAGER, "không khớp", null);

        assertEquals(1, response.getSummary().getTotalProperties());
        assertEquals(0, response.getSummary().getTotalRooms());
        assertEquals(0, response.getSummary().getVacancyRate());
        assertTrue(response.getFacilities().isEmpty());
    }

    private GetFacilitiesDashboardService service(
            ResultProvider propertyResults,
            ResultProvider floorResults,
            ResultProvider roomResults,
            ResultProvider promotionResults
    ) {
        return new GetFacilitiesDashboardService(
                proxy(JpaPropertyRepository.class, propertyResults),
                proxy(JpaFloorRepository.class, floorResults),
                proxy(JpaRoomRepository.class, roomResults),
                proxy(JpaRolePromotionRepository.class, promotionResults)
        );
    }

    private <T> T proxy(Class<T> type, ResultProvider provider) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return type.getSimpleName() + "Stub";
                    }
                    return provider.get(method.getName());
                }
        ));
    }

    private PropertyEntity property(Long id, String code, String name, PropertyStatus status) {
        return PropertyEntity.builder()
                .id(id)
                .propertyCode(code)
                .name(name)
                .propertyType(PropertyType.BOARDING_HOUSE)
                .addressLine("Địa chỉ " + code)
                .status(status)
                .build();
    }

    private FloorEntity floor(Long id, PropertyEntity property, String code, String name) {
        return FloorEntity.builder()
                .id(id)
                .property(property)
                .floorCode(code)
                .name(name)
                .sortOrder(1)
                .status(FloorStatus.ACTIVE)
                .build();
    }

    private RoomEntity room(
            Long id,
            PropertyEntity property,
            FloorEntity floor,
            String code,
            RoomStatus status
    ) {
        return RoomEntity.builder()
                .id(id)
                .property(property)
                .floor(floor)
                .roomCode(code)
                .name("Phòng " + code)
                .currentStatus(status)
                .sortOrder(id.intValue())
                .build();
    }

    @FunctionalInterface
    private interface ResultProvider {
        Object get(String methodName);
    }
}
