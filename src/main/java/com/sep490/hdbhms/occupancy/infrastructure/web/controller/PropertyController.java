package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListPropertiesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListPropertiesUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaRoomRepository;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreatePropertyRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ApiResponse<PageResponse<PropertyResponse>> getProperties(
            @RequestParam(required = false) PropertyStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
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
        return ApiResponse.<List<PropertySimpleResponse>>builder()
                .data(jpaPropertyRepository.findAllByDeletedAtIsNull()
                        .stream()
                        .map(this::toSimpleResponse)
                        .toList())
                .build();
    }

    @GetMapping("/{propertyId}/rooms/simple")
    public ApiResponse<List<RoomSimpleResponse>> getSimpleRoomsByProperty(@PathVariable Long propertyId) {
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

    private PropertySimpleResponse toSimpleResponse(PropertyEntity property) {
        return PropertySimpleResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .propertyCode(property.getPropertyCode())
                .build();
    }
}
