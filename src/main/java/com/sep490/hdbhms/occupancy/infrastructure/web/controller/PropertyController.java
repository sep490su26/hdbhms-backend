package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.occupancy.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListPropertiesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListPropertiesUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreatePropertyRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdatePropertyRequest;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyController {
    PropertyWebMapper propertyWebMapper;
    CreatePropertyUseCase createPropertyUseCase;
    GetListPropertiesUseCase getListPropertiesUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
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
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
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
        property.setStatus(request.status());
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
