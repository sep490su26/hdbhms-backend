package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.FloorEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.FloorPlanItemEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaFloorPlanItemRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaFloorRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.SaveFloorPlanItemRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.request.SaveFloorPlanLayoutRequest;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.FloorPlanItemResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.FloorPlanLayoutResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.PublicPropertyFloorPlanResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FloorPlanController {
    static Set<String> ALLOWED_ITEM_TYPES = Set.of(
            "ROOM",
            "CORRIDOR",
            "STAIR",
            "PARKING",
            "LAUNDRY",
            "GATE",
            "YARD",
            "LABEL",
            "UTILITY"
    );

    JpaPropertyRepository propertyRepository;
    JpaFloorRepository floorRepository;
    JpaRoomRepository roomRepository;
    JpaFloorPlanItemRepository floorPlanItemRepository;
    JpaRolePromotionRepository rolePromotionRepository;
    ObjectMapper objectMapper;

    @GetMapping("/api/v1/admin/properties/{propertyId}/floors/{floorId}/floor-plan")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<FloorPlanLayoutResponse> getAdminFloorPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long propertyId,
            @PathVariable Long floorId
    ) {
        assertCanManageProperty(principal, propertyId);
        PropertyEntity property = requireProperty(propertyId);
        FloorEntity floor = requireFloor(propertyId, floorId);
        return ApiResponse.<FloorPlanLayoutResponse>builder()
                .data(toLayoutResponse(
                        property,
                        floor,
                        floorPlanItemRepository.findAllByProperty_IdAndFloor_IdOrderByIdAsc(propertyId, floorId)
                ))
                .build();
    }

    @PutMapping("/api/v1/admin/properties/{propertyId}/floors/{floorId}/floor-plan")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ApiResponse<FloorPlanLayoutResponse> saveAdminFloorPlan(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long propertyId,
            @PathVariable Long floorId,
            @Valid @RequestBody SaveFloorPlanLayoutRequest request
    ) {
        assertCanManageProperty(principal, propertyId);
        PropertyEntity property = requireProperty(propertyId);
        FloorEntity floor = requireFloor(propertyId, floorId);
        List<FloorPlanItemEntity> nextItems = new ArrayList<>();
        boolean roomAreaAdjusted = false;

        for (SaveFloorPlanItemRequest item : request.items()) {
            String itemType = normalizeItemType(item.type());
            RoomEntity room = null;
            if ("ROOM".equals(itemType)) {
                if (item.roomId() == null) {
                    throw new AppException(ApiErrorCode.INVALID_REQUEST);
                }
                room = roomRepository.findById(item.roomId())
                        .filter(candidate -> candidate.getDeletedAt() == null)
                        .filter(candidate -> Objects.equals(candidate.getProperty().getId(), propertyId))
                        .filter(candidate -> Objects.equals(candidate.getFloor().getId(), floorId))
                        .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_REQUEST));
                roomAreaAdjusted |= syncRoomDetails(room, item.metadata());
            }

            nextItems.add(FloorPlanItemEntity.builder()
                    .property(property)
                    .floor(floor)
                    .room(room)
                    .type(itemType)
                    .positionX(item.positionX())
                    .positionY(item.positionY())
                    .width(item.width())
                    .height(item.height())
                    .metadata(toJson(item.metadata()))
                    .build());
        }

        floorPlanItemRepository.deleteByProperty_IdAndFloor_Id(propertyId, floorId);
        List<FloorPlanItemEntity> savedItems = floorPlanItemRepository.saveAll(nextItems);
        return ApiResponse.<FloorPlanLayoutResponse>builder()
                .message(roomAreaAdjusted ? "Diện tích phòng được điều chỉnh về 4 m²" : "Đã lưu sơ đồ tầng")
                .data(toLayoutResponse(property, floor, savedItems))
                .build();
    }

    @GetMapping("/api/v1/public/properties/{propertyId}/floor-plan")
    public ApiResponse<PublicPropertyFloorPlanResponse> getPublicFloorPlan(@PathVariable Long propertyId) {
        PropertyEntity property = requireProperty(propertyId);
        List<FloorEntity> floors = floorRepository.findAllByProperty_Id(propertyId)
                .stream()
                .filter(floor -> floor.getDeletedAt() == null)
                .sorted(Comparator.comparing(FloorEntity::getSortOrder).thenComparing(FloorEntity::getId))
                .toList();
        Map<Long, List<FloorPlanItemEntity>> itemsByFloor = floorPlanItemRepository
                .findAllByProperty_IdOrderByFloor_SortOrderAscIdAsc(propertyId)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getFloor().getId(), LinkedHashMap::new, Collectors.toList()));

        return ApiResponse.<PublicPropertyFloorPlanResponse>builder()
                .data(new PublicPropertyFloorPlanResponse(
                        property.getId(),
                        property.getName(),
                        floors.stream()
                                .map(floor -> toLayoutResponse(property, floor, itemsByFloor.getOrDefault(floor.getId(), List.of())))
                                .toList()
                ))
                .build();
    }

    private PropertyEntity requireProperty(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .filter(property -> property.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ApiErrorCode.PROPERTY_NOT_FOUND));
    }

    private FloorEntity requireFloor(Long propertyId, Long floorId) {
        return floorRepository.findById(floorId)
                .filter(floor -> floor.getDeletedAt() == null)
                .filter(floor -> Objects.equals(floor.getProperty().getId(), propertyId))
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));
    }

    private void assertCanManageProperty(UserPrincipal principal, Long propertyId) {
        if (principal == null) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        if (principal.getRole() != Role.MANAGER) {
            return;
        }
        List<Long> managerPropertyIds = rolePromotionRepository.findActivePropertyIds(
                principal.getId(),
                PromotionRole.MANAGER,
                RolePromotionStatus.ACTIVE
        );
        if (!managerPropertyIds.contains(propertyId)) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
    }

    private String normalizeItemType(String itemType) {
        String normalized = itemType == null ? "" : itemType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ITEM_TYPES.contains(normalized)) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        return normalized;
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private boolean syncRoomDetails(RoomEntity room, Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return false;
        }

        readNonNegativeLong(metadata.get("listedPrice")).ifPresent(room::setListedPrice);
        readPositiveInteger(metadata.get("maxOccupants")).ifPresent(room::setMaxOccupants);
        Optional<BigDecimal> area = readNonNegativeDecimal(metadata.get("areaSqm"));
        if (area.isEmpty()) {
            return false;
        }
        boolean adjusted = area.get().compareTo(BigDecimal.valueOf(4)) < 0;
        room.setAreaM2(adjusted ? BigDecimal.valueOf(4) : area.get());
        return adjusted;
    }

    private Optional<Long> readNonNegativeLong(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            Long parsed = switch (value) {
                case Number number -> number.longValue();
                case String text -> Long.parseLong(text.replaceAll("[^0-9]", ""));
                default -> null;
            };
            return parsed == null || parsed < 0 ? Optional.empty() : Optional.of(parsed);
        } catch (RuntimeException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private Optional<BigDecimal> readNonNegativeDecimal(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            BigDecimal parsed = switch (value) {
                case BigDecimal decimal -> decimal;
                case Number number -> BigDecimal.valueOf(number.doubleValue());
                case String text -> new BigDecimal(text.trim().replace(",", "."));
                default -> null;
            };
            return parsed == null || parsed.signum() < 0 ? Optional.empty() : Optional.of(parsed);
        } catch (RuntimeException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private Optional<Integer> readPositiveInteger(Object value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            Integer parsed = switch (value) {
                case Number number -> number.intValue();
                case String text -> Integer.parseInt(text.replaceAll("[^0-9]", ""));
                default -> null;
            };
            return parsed == null || parsed < 1 ? Optional.empty() : Optional.of(parsed);
        } catch (RuntimeException exception) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }

    private FloorPlanLayoutResponse toLayoutResponse(
            PropertyEntity property,
            FloorEntity floor,
            List<FloorPlanItemEntity> items
    ) {
        return new FloorPlanLayoutResponse(
                property.getId(),
                property.getName(),
                floor.getId(),
                floor.getName(),
                floor.getSortOrder(),
                items.size(),
                items.stream()
                        .sorted(Comparator.comparingInt(this::metadataSortOrder)
                                .thenComparing(FloorPlanItemEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private int metadataSortOrder(FloorPlanItemEntity item) {
        Object value = fromJson(item.getMetadata()).get("sortOrder");
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.MAX_VALUE;
    }

    private FloorPlanItemResponse toItemResponse(FloorPlanItemEntity item) {
        RoomEntity room = item.getRoom();
        return new FloorPlanItemResponse(
                item.getId(),
                item.getType(),
                room == null ? null : room.getId(),
                room == null ? null : room.getRoomCode(),
                item.getPositionX(),
                item.getPositionY(),
                item.getWidth(),
                item.getHeight(),
                fromJson(item.getMetadata()),
                room == null ? null : room.getCurrentStatus(),
                room == null ? null : room.getListedPrice(),
                room == null ? null : room.getAreaM2(),
                room == null ? null : room.getMaxOccupants()
        );
    }
}
