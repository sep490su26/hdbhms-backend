package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.command.CreateFloorCommand;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetListFloorsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateFloorUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetFloorDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListFloorsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.model.Floor;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateFloorRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.FloorResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.FloorWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/floors")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FloorController {
    FloorWebMapper floorWebMapper;
    CreateFloorUseCase createFloorUseCase;
    GetListFloorsUseCase getListFloorsUseCase;
    GetFloorDetailsUseCase getFloorDetailsUseCase;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;

    @GetMapping
    public ApiResponse<List<FloorResponse>> getFloors(
            @RequestParam Long propertyId
    ) {
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(propertyId)
        );
        return ApiResponse.<List<FloorResponse>>builder()
                .data(
                        getListFloorsUseCase.execute(
                                        new GetListFloorsQuery(propertyId)
                                ).stream()
                                .map(floor -> floorWebMapper.toFloorResponse(floor, property))
                                .toList()
                )
                .build();
    }

    @GetMapping("/{floorId}")
    public ApiResponse<FloorResponse> getFloorDetails(
            @PathVariable Long floorId
    ) {
        Floor floor = getFloorDetailsUseCase.execute(new GetFloorDetailsQuery(floorId));
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(floor.getPropertyId())
        );
        return ApiResponse.<FloorResponse>builder()
                .data(
                        floorWebMapper.toFloorResponse(floor, property)
                )
                .build();
    }

    @PostMapping
    public ApiResponse<FloorResponse> createFloor(@Valid @RequestBody CreateFloorRequest request) {
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(request.getPropertyId())
        );
        return ApiResponse.<FloorResponse>builder()
                .data(
                        floorWebMapper.toFloorResponse(
                                createFloorUseCase.execute(
                                        new CreateFloorCommand(
                                                request.getPropertyId(),
                                                request.getFloorCode(),
                                                request.getName(),
                                                request.getSortOrder()
                                        )
                                ),
                                property
                        )
                )
                .build();
    }
}
