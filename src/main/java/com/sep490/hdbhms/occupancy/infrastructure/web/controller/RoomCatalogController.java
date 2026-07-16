package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomCatalogController {
    private static final Set<RoomStatus> PUBLIC_ROOM_STATUSES = Set.of(
            RoomStatus.VACANT,
            RoomStatus.SOON_VACANT
    );

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
        boolean publicRequest = !hasInternalRoomCatalogAccess();
        Page<RoomResponse> page = roomRepository
                .findAll(roomFilter(propertyId, floorId, status, minPrice, maxPrice, publicRequest), pageable)
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
            Long maxPrice,
            boolean publicRequest
    ) {
        return (root, query, criteriaBuilder) -> {
            if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                root.fetch("property", JoinType.LEFT);
                root.fetch("floor", JoinType.LEFT);
            }

            var predicates = new ArrayList<Predicate>();
            predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            if (publicRequest) {
                predicates.add(criteriaBuilder.isNull(root.get("property").get("deletedAt")));
                predicates.add(criteriaBuilder.equal(
                        root.get("property").get("status"),
                        PropertyStatus.ACTIVE
                ));
                predicates.add(status == null
                        ? root.get("currentStatus").in(PUBLIC_ROOM_STATUSES)
                        : PUBLIC_ROOM_STATUSES.contains(status)
                        ? criteriaBuilder.equal(root.get("currentStatus"), status)
                        : criteriaBuilder.disjunction());
            }
            if (propertyId != null) {
                predicates.add(criteriaBuilder.equal(root.get("property").get("id"), propertyId));
            }
            if (floorId != null) {
                predicates.add(criteriaBuilder.equal(root.get("floor").get("id"), floorId));
            }
            if (!publicRequest && status != null) {
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

    static boolean isPublicRoomStatus(RoomStatus status) {
        return PUBLIC_ROOM_STATUSES.contains(status);
    }

    private boolean hasInternalRoomCatalogAccess() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return false;
        }
        return principal.getRole() == Role.OWNER
                || principal.getRole() == Role.MANAGER
                || principal.getRole() == Role.ACCOUNTANT;
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
