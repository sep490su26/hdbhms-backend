package com.sep490.hdbhms.changerequest.infrastructure.web.controller;

import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestResponse;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestStatsResponse;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.request.RejectRequestRequest;
import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestQueryUseCase;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestController {

    ChangeRequestQueryUseCase queryUseCase;
    ChangeRequestUseCase useCase;

    @GetMapping
    public ApiResponse<PageResponse<ChangeRequestResponse>> getRequests(
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false ) RequestStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChangeRequest> requestPage = queryUseCase.getFilteredRequests(type, status, search, pageable);

        Page<ChangeRequestResponse> responsePage = requestPage.map(req -> new ChangeRequestResponse(
                req.getId(),
                req.getRequestCode(),
                req.getRequestType(),
                req.getTitle(),
                req.getDescription(),
                req.getStatus(),
                req.getRequesterId(),
                req.getResolutionNote(),
                req.getCreatedAt()
        ));

        return ApiResponse.<PageResponse<ChangeRequestResponse>>builder()
                .code(0)
                .data(PageResponse.fromPageToPageResponse(responsePage))
                .build();
    }

    @GetMapping("/stats")
    public ApiResponse<ChangeRequestStatsResponse> getStats() {
        return ApiResponse.<ChangeRequestStatsResponse>builder()
                .code(0)
                .data(queryUseCase.getStats())
                .build();
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approveRequest(
            @PathVariable Long id
    ) {
        Long managerId = AuthUtils.getCurrentAuthenticationId();
        useCase.approveRequest(new ApproveRequestCommand(id, managerId));
        return ApiResponse.<Void>builder()
                .code(0)
                .message("Request approved successfully")
                .build();
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<Void> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectRequestRequest request
    ) {
        Long managerId = AuthUtils.getCurrentAuthenticationId();
        useCase.rejectRequest(new RejectRequestCommand(id, managerId, request.resolutionNote()));
        return ApiResponse.<Void>builder()
                .code(0)
                .message("Request rejected successfully")
                .build();
    }
}
