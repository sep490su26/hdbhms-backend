package com.sep490.hdbhms.occupancy.application.service;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.FacilitiesDashboardResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GetFacilitiesDashboardService {
    JpaPropertyRepository propertyRepository;
    JpaFloorRepository floorRepository;
    JpaRoomRepository roomRepository;
    JpaRolePromotionRepository rolePromotionRepository;

    @Transactional(readOnly = true)
    public FacilitiesDashboardResponse getDashboard(
            Long userId,
            Role role,
            String keyword,
            PropertyStatus status
    ) {
        List<PropertyEntity> scopedProperties = getScopedProperties(userId, role).stream()
                .sorted(Comparator.comparing(PropertyEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<FacilitiesDashboardResponse.Facility> allFacilities = scopedProperties.stream()
                .map(this::toFacility)
                .toList();

        String normalizedKeyword = normalize(keyword);
        List<FacilitiesDashboardResponse.Facility> filteredFacilities = allFacilities.stream()
                .filter(facility -> status == null || facility.getStatus() == status)
                .filter(facility -> matchesKeyword(facility, normalizedKeyword))
                .toList();

        long totalFloors = allFacilities.stream()
                .mapToLong(FacilitiesDashboardResponse.Facility::getFloorCount)
                .sum();
        long totalRooms = allFacilities.stream()
                .mapToLong(FacilitiesDashboardResponse.Facility::getRoomCount)
                .sum();
        long occupiedRooms = allFacilities.stream()
                .mapToLong(FacilitiesDashboardResponse.Facility::getOccupiedRoomCount)
                .sum();
        long vacantRooms = allFacilities.stream()
                .mapToLong(FacilitiesDashboardResponse.Facility::getVacantRoomCount)
                .sum();

        return FacilitiesDashboardResponse.builder()
                .summary(FacilitiesDashboardResponse.Summary.builder()
                        .totalProperties(allFacilities.size())
                        .activeProperties(allFacilities.stream()
                                .filter(facility -> facility.getStatus() == PropertyStatus.ACTIVE)
                                .count())
                        .totalFloors(totalFloors)
                        .totalRooms(totalRooms)
                        .occupiedRooms(occupiedRooms)
                        .vacantRooms(vacantRooms)
                        .vacancyRate(totalRooms == 0 ? 0 : (int) Math.round(vacantRooms * 100.0 / totalRooms))
                        .build())
                .availableStatuses(List.of(PropertyStatus.values()))
                .facilities(filteredFacilities)
                .build();
    }

    private List<PropertyEntity> getScopedProperties(Long userId, Role role) {
        if (role == Role.MANAGER) {
            List<Long> propertyIds = rolePromotionRepository.findActivePropertyIds(
                            userId,
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
        return propertyRepository.findAllByDeletedAtIsNull();
    }

    private FacilitiesDashboardResponse.Facility toFacility(PropertyEntity property) {
        List<FloorEntity> floors = floorRepository.findAllByProperty_Id(property.getId()).stream()
                .filter(floor -> floor.getDeletedAt() == null)
                .sorted(Comparator.comparing(FloorEntity::getSortOrder)
                        .thenComparing(FloorEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<RoomEntity> rooms = roomRepository
                .findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc(property.getId());
        Map<Long, List<RoomEntity>> roomsByFloor = rooms.stream()
                .collect(Collectors.groupingBy(room -> room.getFloor().getId()));

        List<FacilitiesDashboardResponse.Floor> floorResponses = floors.stream()
                .map(floor -> toFloor(floor, roomsByFloor.getOrDefault(floor.getId(), List.of())))
                .toList();

        return FacilitiesDashboardResponse.Facility.builder()
                .id(property.getId())
                .code(property.getPropertyCode())
                .name(property.getName())
                .address(property.getAddressLine())
                .description(property.getDescription())
                .status(property.getStatus())
                .floorCount(floorResponses.size())
                .roomCount(rooms.size())
                .occupiedRoomCount(countRooms(rooms, RoomStatus.OCCUPIED))
                .vacantRoomCount(countRooms(rooms, RoomStatus.VACANT))
                .floors(floorResponses)
                .build();
    }

    private FacilitiesDashboardResponse.Floor toFloor(FloorEntity floor, List<RoomEntity> rooms) {
        List<FacilitiesDashboardResponse.Room> roomResponses = rooms.stream()
                .sorted(Comparator.comparing(RoomEntity::getSortOrder)
                        .thenComparing(RoomEntity::getRoomCode, String.CASE_INSENSITIVE_ORDER))
                .map(room -> FacilitiesDashboardResponse.Room.builder()
                        .id(room.getId())
                        .code(room.getRoomCode())
                        .name(room.getName())
                        .status(room.getCurrentStatus())
                        .sortOrder(room.getSortOrder())
                        .build())
                .toList();

        return FacilitiesDashboardResponse.Floor.builder()
                .id(floor.getId())
                .code(floor.getFloorCode())
                .name(floor.getName())
                .sortOrder(floor.getSortOrder())
                .status(floor.getStatus())
                .roomCount(roomResponses.size())
                .occupiedRoomCount(countRooms(rooms, RoomStatus.OCCUPIED))
                .rooms(roomResponses)
                .build();
    }

    private int countRooms(List<RoomEntity> rooms, RoomStatus status) {
        return (int) rooms.stream()
                .map(RoomEntity::getCurrentStatus)
                .filter(status::equals)
                .count();
    }

    private boolean matchesKeyword(FacilitiesDashboardResponse.Facility facility, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return Stream.of(facility.getName(), facility.getCode(), facility.getAddress())
                .filter(Objects::nonNull)
                .map(this::normalize)
                .anyMatch(value -> value.contains(keyword));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
