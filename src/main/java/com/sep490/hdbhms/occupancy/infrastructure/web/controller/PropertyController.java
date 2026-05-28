package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreatePropertyCommand;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListPropertiesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreatePropertyUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListPropertiesUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreatePropertyRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertyResponse;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyController {
    PropertyWebMapper propertyWebMapper;
    CreatePropertyUseCase createPropertyUseCase;
    GetListPropertiesUseCase getListPropertiesUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;

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
}
