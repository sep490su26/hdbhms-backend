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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetDashboardService implements GetDashboardUseCase {
    JpaPropertyRepository propertyRepository;
    JpaFloorRepository floorRepository;
    JpaRoomRepository roomRepository;
    JpaRolePromotionRepository rolePromotionRepository;

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

        return DashboardResponse.builder()
                .totalRoomCount((long) rooms.size())
                .totalOccupiedRoomCount(countRooms(rooms, RoomStatus.OCCUPIED))
                .totalVacantRoomCount(countRooms(rooms, RoomStatus.VACANT))
                .floorEfficiencies(floorEfficiencies)
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
}
