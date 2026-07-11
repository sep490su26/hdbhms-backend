package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountByIdQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountLoginHistoryQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountsQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.*;
import com.sep490.hdbhms.identityandaccess.application.service.TenantAccountProvisioningService;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.LoginHistoryResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.TenantAccountProvisioningResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.UserResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.UserWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.dto.response.PageResponse;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserWebMapper userWebMapper;
    GetUserUseCase getUserUseCase;
    UpdateUserUseCase updateUserUseCase;
    GetListUsersUseCase getListUsersUseCase;
    CreateStaffUserUseCase createStaffUserUseCase;
    GetUserLoginHistoryListUseCase getUserLoginHistoryListUseCase;
    TenantAccountProvisioningService tenantAccountProvisioningService;

    @PostMapping("/staff")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<UserResponse> createStaffAccount(@Valid @RequestBody UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .data(
                        userWebMapper.toAccountResponse(
                                createStaffUserUseCase.execute(userWebMapper.toCommand(request))
                        )
                )
                .build();
    }

    @GetMapping({"", "/", "/accounts"})
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<PageResponse<UserResponse>> getAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) List<Role> roles,
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        List<Role> effectiveRoles = roles == null || roles.isEmpty()
                ? (role == null ? null : List.of(role))
                : roles;
        var command = new GetAccountsQuery(
                keyword,
                effectiveRoles,
                status,
                pageable
        );
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getListUsersUseCase.execute(command)
                                        .map(userWebMapper::toAccountResponse)
                        )
                )
                .build();
    }

    @GetMapping("/tenant-account-candidates")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    ApiResponse<PageResponse<TenantAccountProvisioningResponse>> getTenantAccountCandidates(
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<TenantAccountProvisioningResponse>>builder()
                .data(tenantAccountProvisioningService.findProvisioningCandidates(pageable))
                .build();
    }

    @PostMapping("/tenant-account-candidates/{contractId}/send")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    ApiResponse<TenantAccountProvisioningResponse> sendTenantAccount(
            @PathVariable Long contractId,
            @RequestParam(defaultValue = "false") boolean retry
    ) {
        return ApiResponse.<TenantAccountProvisioningResponse>builder()
                .data(tenantAccountProvisioningService.provisionPrimaryTenantAccount(contractId, retry))
                .build();
    }

    @PatchMapping("/tenant-account-candidates/{contractId}/profiles/{profileId}/disable")
    @PreAuthorize("hasAnyRole('OWNER','MANAGER')")
    ApiResponse<TenantAccountProvisioningResponse> disableTenantAccountAccess(
            @PathVariable Long contractId,
            @PathVariable Long profileId,
            @RequestBody TenantAccountAccessDisableRequest request
    ) {
        return ApiResponse.<TenantAccountProvisioningResponse>builder()
                .data(tenantAccountProvisioningService.disableTenantContext(
                        contractId,
                        profileId,
                        request == null ? null : request.getReason()
                ))
                .build();
    }

    @GetMapping("/{accountId:\\d+}")
    ApiResponse<UserResponse> getAccount(@PathVariable Long accountId) {
        return ApiResponse.<UserResponse>builder()
                .data(
                        userWebMapper.toAccountResponse(
                                getUserUseCase.getById(new GetAccountByIdQuery(accountId))
                        )
                )
                .build();
    }


    @PatchMapping(value = "/me/email")
    ApiResponse<Void> updateMyEmail(
            @Valid @RequestBody AccountEmailUpdateRequest emailUpdateRequest) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        updateUserUseCase.requestUpdateUserEmail(new UpdateAccountEmailCommand(
                userId,
                emailUpdateRequest.getNewEmail(),
                emailUpdateRequest.getCurrentPassword()
        ));
        return ApiResponse.<Void>builder()
                .build();
    }

    @PostMapping(value = "/me/email/confirm")
    ApiResponse<UserResponse> confirmMyEmailUpdate(
            @Valid @RequestBody AccountEmailUpdateConfirmationRequest emailUpdateConfirmationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<UserResponse>builder()
                .data(
                        userWebMapper.toAccountResponse(
                                updateUserUseCase.confirmUpdateUserEmail(
                                        new VerifyUpdateEmailCommand(
                                                userId,
                                                emailUpdateConfirmationRequest.getOtpCode(),
                                                request,
                                                response
                                        )
                                )
                        )
                )
                .build();
    }

    @PatchMapping(value = "/me/first-password")
    ApiResponse<UserResponse> setMyFirstPassword(
            @Valid @RequestBody AccountFirstPasswordUpdateRequest accountFirstPasswordUpdateRequest
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<UserResponse>builder()
                .data(userWebMapper.toAccountResponse(
                        updateUserUseCase.updateUserFirstPassword(
                                new UpdateUserFirstPasswordCommand(
                                        userId,
                                        accountFirstPasswordUpdateRequest.getNewPassword()
                                ))
                ))
                .build();
    }

    @PatchMapping(value = "/me/password")
    ApiResponse<UserResponse> updateMyPassword(
            @Valid @RequestBody AccountPasswordUpdateRequest usernameUpdateRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long userId = AuthUtils.getCurrentAuthenticationId();
        return ApiResponse.<UserResponse>builder()
                .data(userWebMapper.toAccountResponse(
                        updateUserUseCase.updateUserPassword(new UpdateUserPasswordCommand(
                                userId,
                                usernameUpdateRequest.getCurrentPassword(),
                                usernameUpdateRequest.getNewPassword(),
                                request,
                                response
                        ))
                ))
                .build();
    }

    @PutMapping(value = "/{userId:\\d+}/status")
    ApiResponse<UserResponse> updateAccountStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AccountStatusUpdateRequest request
    ) {
        return ApiResponse.<UserResponse>builder()
                .data(
                        userWebMapper.toAccountResponse(
                                updateUserUseCase.updateUserStatus(
                                        new UpdateAccountStatusCommand(
                                                userId,
                                                request.getStatus()
                                        )
                                )
                        )
                )
                .build();
    }

    @PutMapping(value = "/{accountId:\\d+}/role")
    ApiResponse<UserResponse> updateAccountRole(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountRoleUpdateRequest request
    ) {
        return ApiResponse.<UserResponse>builder()
                .data(
                        userWebMapper.toAccountResponse(
                                updateUserUseCase.updateUserRole(
                                        new UpdateAccountRoleCommand(
                                                accountId,
                                                request.getRole()
                                        )
                                )
                        )
                )
                .build();
    }

    @GetMapping("/{accountId:\\d+}/login-history")
    ApiResponse<PageResponse<LoginHistoryResponse>> getLoginHistory(
            @PathVariable Long accountId,
            @RequestParam(required = false) List<String> statuses,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<LoginHistoryResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getUserLoginHistoryListUseCase.execute(
                                                new GetAccountLoginHistoryQuery(
                                                        accountId,
                                                        statuses,
                                                        pageable
                                                ))
                                        .map(userWebMapper::toLoginHistoryResponse)
                        )
                )
                .build();
    }
}
