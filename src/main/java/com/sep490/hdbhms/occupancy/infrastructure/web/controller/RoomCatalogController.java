package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomCatalogController {
    JpaRoomRepository roomRepository;

    @GetMapping("/api/v1/rooms")
    public ApiResponse<PageResponse<RoomResponse>> getRooms(
            @RequestParam(defaultValue = "1") Long propertyId,
            @RequestParam(required = false) Long floorId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<RoomResponse> page = roomRepository
                .findAll(roomFilter(propertyId, floorId, status, minPrice, maxPrice), pageable)
                .map(this::toResponse);

        return ApiResponse.<PageResponse<RoomResponse>>builder()
                .data(PageResponse.fromPageToPageResponse(page))
                .build();
    }

    private Specification<RoomEntity> roomFilter(
            Long propertyId,
            Long floorId,
            RoomStatus status,
            Long minPrice,
            Long maxPrice
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("property", JoinType.LEFT);
                root.fetch("floor", JoinType.LEFT);
            }

            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            if (propertyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("property").get("id"), propertyId));
            }
            if (floorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("floor").get("id"), floorId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("currentStatus"), status));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("listedPrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("listedPrice"), maxPrice));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private RoomResponse toResponse(RoomEntity room) {
        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .name(room.getName())
                .floorId(room.getFloor().getId())
                .floorName(room.getFloor().getName())
                .propertyId(room.getProperty().getId())
                .propertyName(room.getProperty().getName())
                .listedPrice(room.getListedPrice())
                .areaM2(room.getAreaM2())
                .maxOccupants(room.getMaxOccupants())
                .currentStatus(room.getCurrentStatus())
                .positionX(room.getPositionX())
                .positionY(room.getPositionY())
                .build();
    }
}
