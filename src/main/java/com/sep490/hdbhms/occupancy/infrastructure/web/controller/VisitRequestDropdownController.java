package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertyResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.RoomSimpleResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants/{tenantId}/properties")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestDropdownController {
    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;

    @GetMapping
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<List<PropertyResponse>> getProperties(@PathVariable Long tenantId) {
        return ApiResponse.<List<PropertyResponse>>builder()
                .data(jpaPropertyRepository.findAllByDeletedAtIsNull()
                        .stream()
                        .map(this::toPropertyResponse)
                        .toList())
                .build();
    }

    @GetMapping("/{propertyId}/rooms/simple")
    @PreAuthorize("@visitRequestAccessGuard.canManage(authentication)")
    public ApiResponse<List<RoomSimpleResponse>> getRooms(
            @PathVariable Long tenantId,
            @PathVariable Long propertyId
    ) {
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
}
