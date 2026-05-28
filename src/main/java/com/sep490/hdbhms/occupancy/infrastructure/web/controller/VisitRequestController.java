package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.*;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/visit-requests")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VisitRequestController {
    VisitRequestWebMapper visitRequestWebMapper;
    GetRoomDetailsUseCase getRoomDetailsUseCase;
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
        Room room = getRoomDetailsUseCase.execute(
                new GetRoomDetailsQuery(visitRequest.getRoomId())
        );
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property,
                                room
                        )
                )
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<VisitRequestResponse>> getVisitRequests(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String propertyCode,
            @RequestParam(required = false) String roomCode,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable
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
                                        .map(visitRequest -> {
                                            return VisitRequestResponse.builder()
                                                    .visitorName(visitRequest.getVisitorName())
                                                    .visitorEmail(visitRequest.getVisitorEmail())
                                                    .visitorPhone(visitRequest.getVisitorPhone())
                                                    .createdAt(visitRequest.getCreatedAt())
                                                    .roomName(visitRequest.getRoomId() == null ? null
                                                                    : getRoomDetailsUseCase.execute(
                                                                    new GetRoomDetailsQuery(
                                                                            visitRequest.getRoomId()
                                                                    )
                                                            ).getName()
                                                    )
                                                    .propertyName(visitRequest.getPropertyId() == null ? null
                                                                    : getPropertyDetailsUseCase.execute(
                                                                    new GetPropertyDetailsQuery(
                                                                            visitRequest.getPropertyId()
                                                                    )
                                                            ).getName()
                                                    )
                                                    .preferredStart(visitRequest.getPreferredStart())
                                                    .build();
                                        })
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
        Room room = getRoomDetailsUseCase.execute(
                new GetRoomDetailsQuery(visitRequest.getRoomId())
        );
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                visitRequest,
                                property,
                                room
                        )
                )
                .build();
    }
}
