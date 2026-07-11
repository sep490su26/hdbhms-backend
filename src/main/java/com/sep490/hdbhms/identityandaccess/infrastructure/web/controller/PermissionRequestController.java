package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestQueryUseCase;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.application.port.out.ChangeRequestRepository;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.AssignedRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequesterRole;
import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionTargetType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.CreatePermissionRequestRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.RejectPermissionRequestRequest;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestApprovalResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestRejectionResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.PermissionRequestResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.id.SnowflakeIdGenerator;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permission-requests")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionRequestController {
    ChangeRequestRepository changeRequestRepository;
    ChangeRequestQueryUseCase changeRequestQueryUseCase;
    ChangeRequestUseCase changeRequestUseCase;
    ObjectMapper objectMapper;
    SnowflakeIdGenerator snowflakeIdGenerator;

    @GetMapping
    public ApiResponse<PageResponse<PermissionRequestResponse>> getPermissionRequests(
            @RequestParam PermissionRequestStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        RequestStatus requestStatus = toRequestStatus(status);
        Page<PermissionRequestResponse> page = requestStatus == null
                ? new PageImpl<>(List.of(), pageable, 0)
                : changeRequestQueryUseCase
                .getFilteredRequests(RequestType.PERMISSION_ACCESS, requestStatus, null, pageable)
                .map(this::toResponse);

        return ApiResponse.<PageResponse<PermissionRequestResponse>>builder()
                .data(PageResponse.fromPageToPageResponse(page))
                .build();
    }

    @PostMapping
    public ApiResponse<PermissionRequestResponse> createPermissionRequest(
            @Valid @RequestBody CreatePermissionRequestRequest request
    ) {
        if (request.getTargetType() == null || request.getTargetId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType and targetId are required.");
        }
        Long requesterUserId = AuthUtils.getCurrentAuthenticationId();
        TargetType targetType = toChangeTargetType(request.getTargetType());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("permissionTargetType", request.getTargetType().name());
        payload.put("targetId", request.getTargetId());

        ChangeRequest changeRequest = ChangeRequest.builder()
                .requestCode("PR-" + snowflakeIdGenerator.next())
                .requestType(RequestType.PERMISSION_ACCESS)
                .requesterId(requesterUserId)
                .requesterRole(currentRequesterRole())
                .targetType(targetType)
                .targetId(request.getTargetId())
                .title("Yêu cầu cấp quyền " + request.getTargetType().name())
                .description("Yêu cầu cấp quyền truy cập " + request.getTargetType().name() + " #" + request.getTargetId())
                .requestPayload(toJson(payload))
                .assignedRole(AssignedRole.OWNER)
                .status(RequestStatus.PENDING)
                .build();

        return ApiResponse.<PermissionRequestResponse>builder()
                .data(toResponse(changeRequestRepository.save(changeRequest)))
                .build();
    }

    @PatchMapping("{permissionRequestId}/approve")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PermissionRequestApprovalResponse> approvePermissionRequest(
            @PathVariable Long permissionRequestId
    ) {
        requirePermissionAccessRequest(permissionRequestId);
        changeRequestUseCase.approveRequest(
                new ApproveRequestCommand(permissionRequestId, AuthUtils.getCurrentAuthenticationId())
        );
        ChangeRequest changeRequest = requirePermissionAccessRequest(permissionRequestId);
        return ApiResponse.<PermissionRequestApprovalResponse>builder()
                .data(PermissionRequestApprovalResponse.builder()
                        .id(changeRequest.getId())
                        .status(toPermissionStatus(changeRequest.getStatus()))
                        .decidedAt(changeRequest.getResolvedAt())
                        .build())
                .build();
    }

    @PatchMapping("{permissionRequestId}/reject")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PermissionRequestRejectionResponse> rejectPermissionRequest(
            @PathVariable Long permissionRequestId,
            @Valid @RequestBody RejectPermissionRequestRequest request
    ) {
        requirePermissionAccessRequest(permissionRequestId);
        changeRequestUseCase.rejectRequest(
                new RejectRequestCommand(permissionRequestId, AuthUtils.getCurrentAuthenticationId(), request.getReason())
        );
        ChangeRequest changeRequest = requirePermissionAccessRequest(permissionRequestId);
        return ApiResponse.<PermissionRequestRejectionResponse>builder()
                .data(PermissionRequestRejectionResponse.builder()
                        .id(changeRequest.getId())
                        .rejectedReason(changeRequest.getResolutionNote())
                        .status(toPermissionStatus(changeRequest.getStatus()))
                        .decidedAt(changeRequest.getResolvedAt())
                        .build())
                .build();
    }

    private ChangeRequest requirePermissionAccessRequest(Long id) {
        ChangeRequest changeRequest = changeRequestQueryUseCase.getRequestById(id);
        if (changeRequest.getRequestType() != RequestType.PERMISSION_ACCESS) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Permission request not found.");
        }
        return changeRequest;
    }

    private PermissionRequestResponse toResponse(ChangeRequest changeRequest) {
        return PermissionRequestResponse.builder()
                .id(changeRequest.getId())
                .requesterUserId(changeRequest.getRequesterId())
                .targetType(toPermissionTargetType(changeRequest.getTargetType()))
                .targetId(changeRequest.getTargetId())
                .rejectedReason(changeRequest.getResolutionNote())
                .status(toPermissionStatus(changeRequest.getStatus()))
                .decidedAt(changeRequest.getResolvedAt())
                .createdAt(changeRequest.getCreatedAt())
                .build();
    }

    private RequestStatus toRequestStatus(PermissionRequestStatus status) {
        return switch (status) {
            case PENDING -> RequestStatus.PENDING;
            case APPROVED -> RequestStatus.APPROVED;
            case REJECTED -> RequestStatus.REJECTED;
            case EXPIRED, REVOKED -> null;
        };
    }

    private PermissionRequestStatus toPermissionStatus(RequestStatus status) {
        return switch (status) {
            case APPROVED -> PermissionRequestStatus.APPROVED;
            case REJECTED -> PermissionRequestStatus.REJECTED;
            default -> PermissionRequestStatus.PENDING;
        };
    }

    private TargetType toChangeTargetType(PermissionTargetType targetType) {
        return switch (targetType) {
            case TENANT_PROFILE -> TargetType.TENANT_PROFILE;
            case IDENTITY_DOCUMENT -> TargetType.IDENTITY_DOCUMENT;
            case CONTRACT -> TargetType.CONTRACT;
            case REPORT -> TargetType.REPORT;
            case FILE -> TargetType.FILE;
        };
    }

    private PermissionTargetType toPermissionTargetType(TargetType targetType) {
        return switch (targetType) {
            case TENANT_PROFILE -> PermissionTargetType.TENANT_PROFILE;
            case IDENTITY_DOCUMENT -> PermissionTargetType.IDENTITY_DOCUMENT;
            case CONTRACT -> PermissionTargetType.CONTRACT;
            case REPORT -> PermissionTargetType.REPORT;
            case FILE -> PermissionTargetType.FILE;
            default -> null;
        };
    }

    private RequesterRole currentRequesterRole() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthenticated.");
        }
        Role role = principal.getRole();
        return switch (role) {
            case LEAD -> RequesterRole.LEAD;
            case TENANT -> RequesterRole.TENANT;
            case MANAGER -> RequesterRole.MANAGER;
            case ACCOUNTANT -> RequesterRole.ACCOUNTANT;
            case OWNER -> RequesterRole.OWNER;
        };
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not serialize request payload.");
        }
    }
}
