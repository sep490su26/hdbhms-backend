package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.*;
import com.sep490.hdbhms.occupancy.application.port.out.VisitRequestRepository;
import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import com.sep490.hdbhms.occupancy.domain.value_objects.VisitRequestStatus;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.CreateVisitRequestRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.VisitRequestStatusUpdateRequest;
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
    VisitRequestRepository visitRequestRepository;

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
        Room room = getRoomOrNull(visitRequest.getRoomId());
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
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) VisitRequestStatus status,
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
                                                        propertyId,
                                                        roomId,
                                                        status,
                                                        from,
                                                        to,
                                                        pageable
                                                )
                                        )
                                        .map(visitRequest -> {
                                            Room room = getRoomOrNull(visitRequest.getRoomId());
                                            Property property = getPropertyOrNull(visitRequest.getPropertyId());
                                            return VisitRequestResponse.builder()
                                                    .id(visitRequest.getId())
                                                    .propertyId(visitRequest.getPropertyId())
                                                    .roomId(visitRequest.getRoomId())
                                                    .visitorName(visitRequest.getVisitorName())
                                                    .visitorEmail(visitRequest.getVisitorEmail())
                                                    .visitorPhone(visitRequest.getVisitorPhone())
                                                    .createdAt(visitRequest.getCreatedAt())
                                                    .roomName(room == null ? null : room.getName())
                                                    .propertyName(property == null ? null : property.getName())
                                                    .preferredStart(visitRequest.getPreferredStart())
                                                    .status(visitRequest.getStatus())
                                                    .notes(visitRequest.getNotes())
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
        Room room = getRoomOrNull(visitRequest.getRoomId());
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

    @PatchMapping("/{id}/status")
    public ApiResponse<VisitRequestDetailsResponse> updateVisitRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody VisitRequestStatusUpdateRequest request
    ) {
        VisitRequest current = getVisitRequestDetailsUseCase.execute(
                new GetVisitRequestDetailsQuery(id)
        );
        VisitRequest updated = VisitRequest.builder()
                .id(current.getId())
                .propertyId(current.getPropertyId())
                .roomId(current.getRoomId())
                .visitorName(current.getVisitorName())
                .visitorPhone(current.getVisitorPhone())
                .visitorEmail(current.getVisitorEmail())
                .preferredStart(current.getPreferredStart())
                .status(request.getStatus())
                .notes(current.getNotes())
                .createdAt(current.getCreatedAt())
                .updatedAt(current.getUpdatedAt())
                .deletedAt(current.getDeletedAt())
                .deletedByUserId(current.getDeletedByUserId())
                .build();
        updated = visitRequestRepository.save(updated);

        Property property = getPropertyOrNull(updated.getPropertyId());
        Room room = getRoomOrNull(updated.getRoomId());
        return ApiResponse.<VisitRequestDetailsResponse>builder()
                .data(
                        visitRequestWebMapper.toDetailsResponse(
                                updated,
                                property,
                                room
                        )
                )
                .build();
    }

    private Property getPropertyOrNull(Long propertyId) {
        return propertyId == null ? null : getPropertyDetailsUseCase.execute(new GetPropertyDetailsQuery(propertyId));
    }

    private Room getRoomOrNull(Long roomId) {
        return roomId == null ? null : getRoomDetailsUseCase.execute(new GetRoomDetailsQuery(roomId));
    }
}
