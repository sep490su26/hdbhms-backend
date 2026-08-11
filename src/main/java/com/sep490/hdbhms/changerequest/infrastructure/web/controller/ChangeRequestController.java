package com.sep490.hdbhms.changerequest.infrastructure.web.controller;

import com.sep490.hdbhms.changerequest.application.port.in.command.ApproveRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.command.RejectRequestCommand;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestQueryUseCase;
import com.sep490.hdbhms.changerequest.application.port.in.usecase.ChangeRequestUseCase;
import com.sep490.hdbhms.changerequest.domain.model.ChangeRequest;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestStatus;
import com.sep490.hdbhms.changerequest.domain.value_objects.RequestType;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.request.ApproveRequestRequest;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.application.port.out.PersonProfileRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.UserPrincipal;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.request.RejectRequestRequest;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestResponse;
import com.sep490.hdbhms.changerequest.infrastructure.web.dto.response.ChangeRequestStatsResponse;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/change-requests")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChangeRequestController {

    ChangeRequestQueryUseCase queryUseCase;
    ChangeRequestUseCase useCase;
    PersonProfileRepository personProfileRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
    public ApiResponse<PageResponse<ChangeRequestResponse>> getRequests(
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChangeRequest> requestPage = queryUseCase.getFilteredRequests(type, status, search, pageable);

        Page<ChangeRequestResponse> responsePage = requestPage.map(this::toResponse);

        return ApiResponse.<PageResponse<ChangeRequestResponse>>builder()
                .code(0)
                .data(PageResponse.fromPageToPageResponse(responsePage))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<ChangeRequestResponse>> getMyRequests(
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ChangeRequest> requestPage = queryUseCase.getFilteredRequestsByRequester(
                AuthUtils.getCurrentAuthenticationId(), type, status, search, pageable);

        Page<ChangeRequestResponse> responsePage = requestPage.map(this::toResponse);

        return ApiResponse.<PageResponse<ChangeRequestResponse>>builder()
                .code(0)
                .data(PageResponse.fromPageToPageResponse(responsePage))
                .build();
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
    public ApiResponse<ChangeRequestStatsResponse> getStats() {
        return ApiResponse.<ChangeRequestStatsResponse>builder()
                .code(0)
                .data(queryUseCase.getStats())
                .build();
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
    public ApiResponse<Void> approveRequest(
            @PathVariable Long id,
            @RequestBody(required = false) ApproveRequestRequest request
    ) {
        assertCanResolve(queryUseCase.getRequestById(id));
        Long managerId = AuthUtils.getCurrentAuthenticationId();
        useCase.approveRequest(new ApproveRequestCommand(
                id,
                managerId,
                request == null ? null : request.durationCode()
        ));
        return ApiResponse.<Void>builder()
                .code(0)
                .message("Phê duyệt yêu cầu thay đổi thành công")
                .build();
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER','ACCOUNTANT')")
    public ApiResponse<Void> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectRequestRequest request
    ) {
        assertCanResolve(queryUseCase.getRequestById(id));
        Long managerId = AuthUtils.getCurrentAuthenticationId();
        useCase.rejectRequest(new RejectRequestCommand(id, managerId, request.resolutionNote()));
        return ApiResponse.<Void>builder()
                .code(0)
                .message("Từ chối yêu cầu thay đổi thành công")
                .build();
    }

    @PostMapping("/{id}/liquidation/deposit-refund/confirm-receipt")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<ChangeRequestResponse> confirmLiquidationDepositReceipt(@PathVariable Long id) {
        ChangeRequest request = useCase.confirmLiquidationDepositReceipt(
                id,
                AuthUtils.getCurrentAuthenticationId()
        );
        return ApiResponse.<ChangeRequestResponse>builder()
                .code(0)
                .data(toResponse(request))
                .build();
    }

    @PostMapping("/{id}/liquidation/deposit-refund/dispute")
    @PreAuthorize("hasRole('TENANT')")
    public ApiResponse<ChangeRequestResponse> disputeLiquidationDepositRefund(
            @PathVariable Long id,
            @RequestBody(required = false) DepositRefundDisputeRequest request
    ) {
        ChangeRequest changeRequest = useCase.disputeLiquidationDepositRefund(
                id,
                AuthUtils.getCurrentAuthenticationId(),
                request == null ? null : request.reason()
        );
        return ApiResponse.<ChangeRequestResponse>builder()
                .code(0)
                .data(toResponse(changeRequest))
                .build();
    }

    private void assertCanResolve(ChangeRequest request) {
        if (request.getRequestType() != RequestType.TENANT_PROFILE_ACCESS
                && request.getRequestType() != RequestType.PERMISSION_ACCESS
                && request.getRequestType() != RequestType.CONTRACT_LIQUIDATION
                && request.getRequestType() != RequestType.CONTRACT_RENEWAL
                && request.getRequestType() != RequestType.ADD_CO_OCCUPANT) {
            return;
        }
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
        if (request.getRequestType() == RequestType.CONTRACT_RENEWAL
                && (principal.getRole() == Role.OWNER || principal.getRole() == Role.MANAGER)) {
            return;
        }
        if (principal.getRole() != Role.OWNER) {
            throw new AppException(ApiErrorCode.FORBIDDEN_OPERATION);
        }
    }

    private ChangeRequestResponse toResponse(ChangeRequest req) {
        PersonProfile requesterProfile = req.getRequesterId() == null
                ? null
                : personProfileRepository.findByUserId(req.getRequesterId()).orElse(null);
        return new ChangeRequestResponse(
                req.getId(),
                req.getRequestCode(),
                req.getRequestType(),
                req.getTargetType(),
                req.getTargetId(),
                req.getTitle(),
                req.getDescription(),
                req.getRequestPayload(),
                req.getStatus(),
                req.getRequesterId(),
                requesterProfile == null ? null : requesterProfile.getFullName(),
                requesterProfile == null ? null : requesterProfile.getPhone(),
                req.getResolutionNote(),
                req.getResolvedAt(),
                req.getCreatedAt()
        );
    }

    record DepositRefundDisputeRequest(String reason) {
    }
}
