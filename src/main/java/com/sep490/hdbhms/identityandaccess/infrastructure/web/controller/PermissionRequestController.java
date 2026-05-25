package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.ApprovePermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.CreatePermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.RejectPermissionRequestCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetListPermissionRequestsQuery;
import com.sep490.hdbhms.identityandaccess.application.service.CreatePermissionRequestService;
import com.sep490.hdbhms.identityandaccess.application.service.GetListPermissionRequestsService;
import com.sep490.hdbhms.identityandaccess.application.service.ResolvePermissionRequestService;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.CreatePermissionRequestRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.RejectPermissionRequestRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestApprovalResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestRejectionResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.PermissionRequestWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permission-requests")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionRequestController {
    PermissionRequestWebMapper permissionRequestWebMapper;
    CreatePermissionRequestService createPermissionRequestService;
    ResolvePermissionRequestService resolvePermissionRequestService;
    GetListPermissionRequestsService getListPermissionRequestsService;

    @GetMapping
    public ApiResponse<PageResponse<PermissionRequestResponse>> getPermissionRequests(
            @RequestParam PermissionRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<PermissionRequestResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListPermissionRequestsService.execute(
                                                new GetListPermissionRequestsQuery(
                                                        status,
                                                        pageable
                                                )
                                        )
                                        .map(permissionRequestWebMapper::toResponse)
                        )
                )
                .build();
    }

    @PostMapping
    public ApiResponse<PermissionRequestResponse> createPermissionRequest(
            @Valid @RequestBody CreatePermissionRequestRequest request
    ) {
        Long requesterUserId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<PermissionRequestResponse>builder()
                .data(
                        permissionRequestWebMapper.toResponse(
                                createPermissionRequestService.execute(
                                        new CreatePermissionRequestCommand(
                                                requesterUserId,
                                                request.getTargetType(),
                                                request.getTargetId()
                                        )
                                )
                        )
                )
                .build();
    }

    @PatchMapping("{permissionRequestId}/approve")
    public ApiResponse<PermissionRequestApprovalResponse> approvePermissionRequest(
            @PathVariable Long permissionRequestId
    ) {
        return ApiResponse.<PermissionRequestApprovalResponse>builder()
                .data(
                        permissionRequestWebMapper.toApprovalResponse(
                                resolvePermissionRequestService.approve(
                                        new ApprovePermissionRequestCommand(permissionRequestId)
                                )
                        )
                )
                .build();
    }

    @PatchMapping("{permissionRequestId}/reject")
    public ApiResponse<PermissionRequestRejectionResponse> rejectPermissionRequest(
            @PathVariable Long permissionRequestId,
            @Valid @RequestBody RejectPermissionRequestRequest request
    ) {
        return ApiResponse.<PermissionRequestRejectionResponse>builder()
                .data(
                        permissionRequestWebMapper.toRejectionResponse(
                                resolvePermissionRequestService.reject(
                                        new RejectPermissionRequestCommand(
                                                permissionRequestId,
                                                request.getReason()
                                        )
                                )
                        )
                )
                .build();
    }
}
