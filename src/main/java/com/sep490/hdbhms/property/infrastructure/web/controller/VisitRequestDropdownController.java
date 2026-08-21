package com.sep490.hdbhms.property.infrastructure.web.controller;

import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.RoomEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.property.application.service.RoomCommitmentChecker;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.PropertyResponse;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.RoomSimpleResponse;
import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tenants/{tenantId}/properties")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestDropdownController {
    private static final List<RoomStatus> VIEWABLE_ROOM_STATUSES = List.of(RoomStatus.VACANT, RoomStatus.SOON_VACANT);

    JpaPropertyRepository jpaPropertyRepository;
    JpaRoomRepository jpaRoomRepository;
    RoomCommitmentChecker roomCommitmentChecker;

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
                .data(jpaRoomRepository.findAllByProperty_IdAndDeletedAtIsNullAndCurrentStatusInOrderBySortOrderAscRoomCodeAsc(
                                propertyId,
                                VIEWABLE_ROOM_STATUSES
                        )
                        .stream()
                        .map(room -> RoomSimpleResponse.builder()
                                .id(room.getId())
                                .roomCode(room.getRoomCode())
                                .name(room.getName())
                                .propertyId(room.getProperty().getId())
                                .status(room.getCurrentStatus())
                                .listedPrice(room.getListedPrice())
                                .expectedVacantDate(expectedVacantDate(room))
                                .build())
                        .toList())
                .build();
    }

    private LocalDate expectedVacantDate(RoomEntity room) {
        if (room.getCurrentStatus() != RoomStatus.SOON_VACANT) {
            return null;
        }
        return roomCommitmentChecker.findExpectedVacantDateForBooking(room.getId()).orElse(null);
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
