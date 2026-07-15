package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListPropertiesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListPropertiesUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.UtilityTariffEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaFloorPlanItemRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaUtilityTariffRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreatePropertyRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.PropertyUtilitySettingsRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdatePropertyRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdatePropertyStatusRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertyUtilitySettingsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertySimpleResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertyResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomSimpleResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.PropertyWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyController {
    private static final List<LeaseStatus> ROOM_OCCUPANCY_CONTRACT_STATUSES = List.of(
            LeaseStatus.ACTIVE,
            LeaseStatus.EXPIRING_SOON,
            LeaseStatus.TERMINATION_PENDING
    );

    PropertyWebMapper propertyWebMapper;
    CreatePropertyUseCase createPropertyUseCase;
    GetListPropertiesUseCase getListPropertiesUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;
    JpaFloorPlanItemRepository jpaFloorPlanItemRepository;
    JpaUtilityTariffRepository jpaUtilityTariffRepository;
    JpaRolePromotionRepository jpaRolePromotionRepository;

    @GetMapping
    public ApiResponse<PageResponse<PropertyResponse>> getProperties(
            @RequestParam(required = false) PropertyStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        if (currentRole() == Role.MANAGER) {
            List<PropertyResponse> properties = jpaPropertyRepository.findAllByIdInAndDeletedAtIsNull(managerPropertyIds())
                    .stream()
                    .filter(property -> status == null || property.getStatus() == status)
                    .map(this::toPropertyResponse)
                    .toList();
            return ApiResponse.<PageResponse<PropertyResponse>>builder()
                    .data(PageResponse.<PropertyResponse>builder()
                            .data(properties)
                            .pageSize(pageable.getPageSize())
                            .currentPage(pageable.getPageNumber() + 1)
                            .totalPages(properties.isEmpty() ? 0 : 1)
                            .totalElements(properties.size())
                            .build())
                    .build();
        }
        return ApiResponse.<PageResponse<PropertyResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListPropertiesUseCase.execute(
                                                new GetListPropertiesQuery(status, pageable)
                                        )
                                        .map(propertyWebMapper::toResponse)
                        )
                )
                .build();
    }

    @GetMapping("/{propertyId}")
    public ApiResponse<PropertyResponse> getProperty(@PathVariable Long propertyId) {
        assertManagerCanAccessProperty(propertyId);
        return ApiResponse.<PropertyResponse>builder()
                .data(
                        propertyWebMapper.toResponse(
                                getPropertyDetailsUseCase.execute(
                                        new GetPropertyDetailsQuery(
                                                propertyId
                                        )
                                )
                        )
                )
                .build();
    }

    @GetMapping("/simple")
    public ApiResponse<List<PropertySimpleResponse>> getSimpleProperties() {
        List<PropertyEntity> properties = currentRole() == Role.MANAGER
                ? jpaPropertyRepository.findAllByIdInAndDeletedAtIsNull(managerPropertyIds())
                : jpaPropertyRepository.findAllByDeletedAtIsNull();
        return ApiResponse.<List<PropertySimpleResponse>>builder()
                .data(properties
                        .stream()
                        .map(this::toSimpleResponse)
                        .toList())
                .build();
    }

    @GetMapping("/{propertyId}/rooms/simple")
    public ApiResponse<List<RoomSimpleResponse>> getSimpleRoomsByProperty(@PathVariable Long propertyId) {
        assertManagerCanAccessProperty(propertyId);
        return ApiResponse.<List<RoomSimpleResponse>>builder()
                .data(jpaRoomRepository.findAllByProperty_IdAndDeletedAtIsNullOrderBySortOrderAscRoomCodeAsc(propertyId)
                        .stream()
                        .map(room -> RoomSimpleResponse.builder()
                                .id(room.getId())
                                .roomCode(room.getRoomCode())
                                .name(room.getName())
                                .propertyId(room.getProperty().getId())
                                .status(room.getCurrentStatus())
                                .listedPrice(room.getListedPrice())
                                .build())
                        .toList())
                .build();
    }

    @GetMapping("/{propertyId}/utility-settings")
    @Transactional(readOnly = true)
    public ApiResponse<PropertyUtilitySettingsResponse> getUtilitySettings(@PathVariable Long propertyId) {
        assertManagerCanAccessProperty(propertyId);
        return ApiResponse.<PropertyUtilitySettingsResponse>builder()
                .data(buildUtilitySettingsResponse(findProperty(propertyId)))
                .build();
    }

    @PutMapping("/{propertyId}/utility-settings")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PropertyUtilitySettingsResponse> updateUtilitySettings(
            @PathVariable Long propertyId,
            @RequestBody PropertyUtilitySettingsRequest request
    ) {
        assertManagerCanAccessProperty(propertyId);
        PropertyEntity property = findProperty(propertyId);
        upsertTariff(
                property,
                UtilityType.ELECTRICITY,
                request.getElectricityUnitPrice(),
                request.getElectricityFreeAllowance()
        );
        upsertTariff(
                property,
                UtilityType.WATER,
                request.getWaterUnitPrice(),
                request.getWaterFreeAllowance()
        );

        return ApiResponse.<PropertyUtilitySettingsResponse>builder()
                .data(buildUtilitySettingsResponse(property))
                .build();
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PropertyResponse> createProperty(
            @Valid @RequestBody CreatePropertyRequest request
    ) {
        return ApiResponse.<PropertyResponse>builder()
                .data(
                        propertyWebMapper.toResponse(
                                createPropertyUseCase.execute(
                                        new CreatePropertyCommand(
                                                request.getName(),
                                                request.getPropertyType(),
                                                request.getAddressLine(),
                                                request.getDescription()
                                        )
                                )
                        )
                )
                .build();
    }

    @PutMapping("/{propertyId}")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PropertyResponse> updateProperty(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        assertManagerCanAccessProperty(propertyId);
        PropertyEntity property = jpaPropertyRepository.findById(propertyId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở."));
        property.setName(request.name().trim());
        property.setPropertyType(request.propertyType());
        property.setAddressLine(request.addressLine().trim());
        property.setDescription(request.description());
        applyPropertyStatusChange(property, request.status());
        return ApiResponse.<PropertyResponse>builder()
                .data(toPropertyResponse(jpaPropertyRepository.save(property)))
                .build();
    }

    @PatchMapping("/{propertyId}/status")
    @Transactional
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PropertyResponse> updatePropertyStatus(
            @PathVariable Long propertyId,
            @Valid @RequestBody UpdatePropertyStatusRequest request
    ) {
        assertManagerCanAccessProperty(propertyId);
        PropertyEntity property = findProperty(propertyId);
        applyPropertyStatusChange(property, request.status());
        return ApiResponse.<PropertyResponse>builder()
                .data(toPropertyResponse(jpaPropertyRepository.save(property)))
                .build();
    }

    private PropertySimpleResponse toSimpleResponse(PropertyEntity property) {
        return PropertySimpleResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .propertyCode(property.getPropertyCode())
                .build();
    }

    private PropertyResponse toPropertyResponse(PropertyEntity property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .propertyCode(property.getPropertyCode())
                .name(property.getName())
                .propertyType(property.getPropertyType())
                .addressLine(property.getAddressLine())
                .description(property.getDescription())
                .status(property.getStatus())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .deletedAt(property.getDeletedAt())
                .build();
    }

    private PropertyEntity findProperty(Long propertyId) {
        return jpaPropertyRepository.findById(propertyId)
                .filter(property -> property.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cơ sở."));
    }

    private void validateCanChangeStatus(PropertyEntity property, PropertyStatus nextStatus) {
        if (property.getStatus() == nextStatus) {
            return;
        }

        boolean hasRooms = jpaRoomRepository.existsByProperty_IdAndDeletedAtIsNull(property.getId());
        boolean hasFloorPlan = jpaFloorPlanItemRepository.existsByProperty_Id(property.getId());
        if (!hasRooms && !hasFloorPlan) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cơ sở cần có sơ đồ tầng và ít nhất một phòng trước khi đổi trạng thái."
            );
        }
        if (!hasFloorPlan) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cơ sở chưa có sơ đồ tầng nên không thể đổi trạng thái."
            );
        }
        if (!hasRooms) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cơ sở chưa có phòng nên không thể đổi trạng thái."
            );
        }
    }

    private void applyPropertyStatusChange(PropertyEntity property, PropertyStatus nextStatus) {
        boolean activatingProperty = property.getStatus() != PropertyStatus.ACTIVE
                && nextStatus == PropertyStatus.ACTIVE;
        validateCanChangeStatus(property, nextStatus);
        property.setStatus(nextStatus);
        if (activatingProperty) {
            jpaRoomRepository.updateRoomsWithoutActiveContractsToStatus(
                    property.getId(),
                    ROOM_OCCUPANCY_CONTRACT_STATUSES,
                    RoomStatus.VACANT
            );
        }
    }

    private PropertyUtilitySettingsResponse buildUtilitySettingsResponse(PropertyEntity property) {
        return PropertyUtilitySettingsResponse.builder()
                .propertyId(property.getId())
                .propertyName(property.getName())
                .electricity(buildUtilitySetting(property, UtilityType.ELECTRICITY))
                .water(buildUtilitySetting(property, UtilityType.WATER))
                .build();
    }

    private PropertyUtilitySettingsResponse.UtilitySetting buildUtilitySetting(
            PropertyEntity property,
            UtilityType utilityType
    ) {
        LocalDate today = LocalDate.now();
        UtilityTariffEntity tariff = findEffectiveTariff(property.getId(), utilityType, today);
        return PropertyUtilitySettingsResponse.UtilitySetting.builder()
                .unitPrice(tariff == null ? defaultUnitPrice(utilityType) : tariff.getUnitPrice())
                .freeAllowance(tariff == null ? defaultFreeAllowance(utilityType) : tariff.getFreeAllowance())
                .effectiveFrom(tariff == null ? null : tariff.getEffectiveFrom())
                .effectiveTo(tariff == null ? null : tariff.getEffectiveTo())
                .build();
    }

    private void upsertTariff(
            PropertyEntity property,
            UtilityType utilityType,
            Long unitPrice,
            Long freeAllowance
    ) {
        LocalDate today = LocalDate.now();
        UtilityTariffEntity current = findEffectiveTariff(property.getId(), utilityType, today);
        Long nextUnitPrice = nonNegative(unitPrice, current == null ? defaultUnitPrice(utilityType) : current.getUnitPrice());
        Long nextFreeAllowance = nonNegative(
                freeAllowance,
                current == null ? defaultFreeAllowance(utilityType) : current.getFreeAllowance()
        );

        if (current != null
                && Objects.equals(current.getUnitPrice(), nextUnitPrice)
                && Objects.equals(current.getFreeAllowance(), nextFreeAllowance)) {
            return;
        }

        if (current != null && today.equals(current.getEffectiveFrom())) {
            current.setUnitPrice(nextUnitPrice);
            current.setFreeAllowance(nextFreeAllowance);
            return;
        }

        if (current != null) {
            current.setEffectiveTo(today.minusDays(1));
        }

        jpaUtilityTariffRepository.save(UtilityTariffEntity.builder()
                .property(property)
                .utilityType(utilityType)
                .unitPrice(nextUnitPrice)
                .freeAllowance(nextFreeAllowance)
                .effectiveFrom(today)
                .build());
    }

    private UtilityTariffEntity findEffectiveTariff(Long propertyId, UtilityType utilityType, LocalDate date) {
        return jpaUtilityTariffRepository.findEffectiveTariffs(propertyId, utilityType, date)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Long defaultUnitPrice(UtilityType utilityType) {
        return switch (utilityType) {
            case ELECTRICITY -> 3500L;
            case WATER -> 20000L;
            default -> 0L;
        };
    }

    private Long defaultFreeAllowance(UtilityType utilityType) {
        return utilityType == UtilityType.WATER ? 6L : 0L;
    }

    private Long nonNegative(Long value, Long fallback) {
        Long nextValue = value == null ? fallback : value;
        if (nextValue < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá trị điện nước không được âm.");
        }
        return nextValue;
    }

    private void assertManagerCanAccessProperty(Long propertyId) {
        if (currentRole() != Role.MANAGER) {
            return;
        }
        if (propertyId == null || !managerPropertyIds().contains(propertyId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem cơ sở này.");
        }
    }

    private List<Long> managerPropertyIds() {
        Long userId = currentUserId();
        if (userId == null) {
            return List.of();
        }
        return jpaRolePromotionRepository
                .findActivePropertyIds(userId, PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Role currentRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getRole();
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getId();
    }
}
