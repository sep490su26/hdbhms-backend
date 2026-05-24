package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.CreateVisitRequestUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetListVisitRequestsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyDetailsUseCase;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetVisitRequestDetailsUseCase;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateVisitRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.VisitRequestDetailsResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.VisitRequestResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.VisitRequestWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-requests")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestController {
    VisitRequestWebMapper visitRequestWebMapper;
    GetPropertyDetailsUseCase getPropertyDetailsUseCase;
    CreateVisitRequestUseCase createVisitRequestUseCase;
    GetListVisitRequestsUseCase getListVisitRequestsUseCase;
    GetVisitRequestDetailsUseCase getVisitRequestDetailsUseCase;

    @PostMapping
    public ApiResponse<VisitRequestDetailsResponse> createVisitRequest(
            @Valid @RequestBody CreateVisitRequestRequest request
    ) {
        VisitRequest visitRequest = createVisitRequestUseCase.execute(
                visitRequestWebMapper.toCommand(request)
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(visitRequest.getPropertyId())
        );
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<VisitRequestResponse>> getVisitRequests(
            String keyword,
            String propertyCode,
            String roomCode,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return ApiResponse.<PageResponse<VisitRequestResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListVisitRequestsUseCase.execute(
                                                new GetListVisitRequestsQuery(
                                                        keyword,
                                                        propertyCode,
                                                        roomCode,
                                                        from,
                                                        to,
                                                        pageable
                                                )
                                        )
                                        .map(visitRequestWebMapper::toResponse)
                        )
                )
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<VisitRequestDetailsResponse> getVisitRequestDetails(
            @PathVariable Long id
    ) {
        VisitRequest visitRequest = getVisitRequestDetailsUseCase.execute(
                new GetVisitRequestDetailsQuery(id)
        );
        Property property = getPropertyDetailsUseCase.execute(
                new GetPropertyDetailsQuery(visitRequest.getPropertyId())
        );
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property
                        )
                )
                .build();
    }
}
