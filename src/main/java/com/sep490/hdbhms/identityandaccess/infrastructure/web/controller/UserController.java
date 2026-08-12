package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountByIdQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountLoginHistoryQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountsQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.*;
import com.sep490.hdbhms.identityandaccess.application.service.TenantAccountProvisioningService;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.RolePromotionEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaRolePromotionRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.LoginHistoryResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.TenantAccountProvisioningResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.UserResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.UserWebMapper;
import com.sep490.hdbhms.property.infrastructure.persistence.entity.PropertyEntity;
import com.sep490.hdbhms.property.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.shared.types.dto.response.ApiResponse;
import com.sep490.hdbhms.shared.types.dto.response.PageResponse;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    JpaUserRepository jpaUserRepository;
    JpaPropertyRepository jpaPropertyRepository;
    JpaRolePromotionRepository jpaRolePromotionRepository;
    JdbcTemplate jdbcTemplate;

    @PostMapping("/staff")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<UserResponse> createStaffAccount(@Valid @RequestBody UserCreationRequest request) {
        UserResponse response = userWebMapper.toAccountResponse(
                createStaffUserUseCase.execute(userWebMapper.toCommand(request))
        );
        if (request.getPropertyId() != null) {
            assignManagerProperty(response.getId(), request.getPropertyId());
            response = enrichAssignedProperties(response);
        }
        return ApiResponse.<UserResponse>builder()
                .message("Đã tạo tài khoản quản lý và gửi thông tin đăng nhập qua email.")
                .details("Tạo tài khoản quản lý thành công")
                .data(response)
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
                                        .map(this::enrichAssignedProperties)
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
                .message("Đã gửi thông tin tài khoản khách thuê thành công")
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
                .message("Vô hiệu hóa quyền truy cập của người thuê thành công")
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
                        enrichAssignedProperties(
                                userWebMapper.toAccountResponse(
                                        getUserUseCase.getById(new GetAccountByIdQuery(accountId))
                                )
                        )
                )
                .build();
    }

    @PutMapping("/{accountId:\\d+}/assigned-property")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<UserResponse> updateAssignedProperty(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountPropertyAssignmentRequest request
    ) {
        boolean alreadyAssigned = jpaRolePromotionRepository
                .findActiveAssignments(accountId, PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .stream()
                .anyMatch(assignment -> assignment.getProperty() != null
                        && request.getPropertyId().equals(assignment.getProperty().getId()));
        assignManagerProperty(accountId, request.getPropertyId());
        return ApiResponse.<UserResponse>builder()
                .message(alreadyAssigned
                        ? "Cập nhật cơ sở được phân công thành công"
                        : "Gán cơ sở cho tài khoản quản lý thành công")
                .data(
                        enrichAssignedProperties(
                                userWebMapper.toAccountResponse(
                                        getUserUseCase.getById(new GetAccountByIdQuery(accountId))
                                )
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
                .message("Đã gửi mã OTP đến email mới")
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
                .message("Cập nhật email thành công")
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
                .message("Thiết lập mật khẩu lần đầu thành công")
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
                .message("Đổi mật khẩu thành công")
                .details("Đổi mật khẩu thành công")
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
                .message("Cập nhật trạng thái tài khoản thành công")
                .data(
                        enrichAssignedProperties(
                                userWebMapper.toAccountResponse(
                                        updateUserUseCase.updateUserStatus(
                                                new UpdateAccountStatusCommand(
                                                        userId,
                                                        request.getStatus()
                                                )
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
                .message("Cập nhật vai trò tài khoản thành công")
                .data(
                        enrichAssignedProperties(
                                userWebMapper.toAccountResponse(
                                        updateUserUseCase.updateUserRole(
                                                new UpdateAccountRoleCommand(
                                                        accountId,
                                                        request.getRole()
                                                )
                                        )
                                )
                        )
                )
                .build();
    }

    private UserResponse enrichAssignedProperties(UserResponse response) {
        if (response == null || response.getId() == null || response.getRole() != Role.MANAGER) {
            return response;
        }
        Map<Long, UserResponse.AssignedPropertyResponse> assignedById = new LinkedHashMap<>();
        jpaRolePromotionRepository
                .findActiveAssignments(response.getId(), PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .stream()
                .map(RolePromotionEntity::getProperty)
                .map(this::toAssignedPropertyResponse)
                .forEach(property -> assignedById.putIfAbsent(property.getId(), property));
        findActiveStaffAssignments(response.getId())
                .forEach(property -> assignedById.putIfAbsent(property.getId(), property));
        response.setAssignedProperties(List.copyOf(assignedById.values()));
        return response;
    }

    private UserResponse.AssignedPropertyResponse toAssignedPropertyResponse(PropertyEntity property) {
        return UserResponse.AssignedPropertyResponse.builder()
                .id(property.getId())
                .name(property.getName())
                .code(property.getPropertyCode())
                .build();
    }

    private List<UserResponse.AssignedPropertyResponse> findActiveStaffAssignments(Long accountId) {
        return jdbcTemplate.query("""
                        SELECT p.property_id, p.name, p.property_code
                        FROM property_staff_assignments psa
                        JOIN properties p
                          ON p.property_id = psa.property_id
                         AND p.deleted_at IS NULL
                        WHERE psa.staff_user_id = ?
                          AND psa.assigned_role = 'MANAGER'
                          AND psa.assignment_status = 'ACTIVE'
                        ORDER BY psa.is_primary DESC, p.name ASC
                        """,
                (rs, rowNum) -> UserResponse.AssignedPropertyResponse.builder()
                        .id(rs.getLong("property_id"))
                        .name(rs.getString("name"))
                        .code(rs.getString("property_code"))
                        .build(),
                accountId);
    }

    private void assignManagerProperty(Long accountId, Long propertyId) {
        UserEntity user = jpaUserRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        if (user.getRole() != Role.MANAGER) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST);
        }
        PropertyEntity property = jpaPropertyRepository.findById(propertyId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ApiErrorCode.RESOURCE_NOT_FOUND));

        jpaRolePromotionRepository
                .findActiveAssignments(accountId, PromotionRole.MANAGER, RolePromotionStatus.ACTIVE)
                .forEach(assignment -> {
                    if (!propertyId.equals(assignment.getProperty().getId())) {
                        assignment.setStatus(RolePromotionStatus.DISABLED);
                        assignment.setDeletedAt(LocalDateTime.now());
                        jpaRolePromotionRepository.save(assignment);
                    }
                });

        RolePromotionEntity assignment = jpaRolePromotionRepository
                .findFirstByUser_IdAndProperty_IdAndRoleAndDeletedAtIsNull(accountId, propertyId, PromotionRole.MANAGER)
                .orElseGet(() -> RolePromotionEntity.builder()
                        .user(user)
                        .property(property)
                        .role(PromotionRole.MANAGER)
                        .build());
        assignment.setStatus(RolePromotionStatus.ACTIVE);
        assignment.setApprovedAt(LocalDateTime.now());
        jpaRolePromotionRepository.save(assignment);
        syncStaffPropertyAssignment(accountId, propertyId);
    }

    private void syncStaffPropertyAssignment(Long accountId, Long propertyId) {
        Long currentUserId = AuthUtils.getCurrentAuthenticationId();

        jdbcTemplate.update("""
                        UPDATE property_staff_assignments
                           SET assignment_status = 'REMOVED',
                               is_primary = FALSE,
                               ended_at = COALESCE(ended_at, NOW(6)),
                               updated_at = NOW(6)
                         WHERE staff_user_id = ?
                           AND assigned_role = 'MANAGER'
                           AND assignment_status = 'ACTIVE'
                           AND property_id <> ?
                        """,
                accountId,
                propertyId);

        Integer activePrimaryCount = jdbcTemplate.queryForObject("""
                        SELECT COUNT(*)
                          FROM property_staff_assignments
                         WHERE property_id = ?
                           AND assigned_role = 'MANAGER'
                           AND assignment_status = 'ACTIVE'
                           AND is_primary = TRUE
                           AND staff_user_id <> ?
                        """,
                Integer.class,
                propertyId,
                accountId);
        boolean isPrimary = activePrimaryCount == null || activePrimaryCount == 0;

        int updated = jdbcTemplate.update("""
                        UPDATE property_staff_assignments
                           SET assignment_status = 'ACTIVE',
                               is_primary = ?,
                               ended_at = NULL,
                               updated_at = NOW(6),
                               assigned_by_user_id = COALESCE(assigned_by_user_id, ?)
                         WHERE property_id = ?
                           AND staff_user_id = ?
                           AND assigned_role = 'MANAGER'
                         ORDER BY property_staff_assignment_id DESC
                         LIMIT 1
                        """,
                isPrimary,
                currentUserId,
                propertyId,
                accountId);

        if (updated == 0) {
            jdbcTemplate.update("""
                            INSERT INTO property_staff_assignments
                                (property_id, staff_user_id, assigned_role, assignment_status, is_primary, notes, assigned_by_user_id, started_at, ended_at, created_at, updated_at)
                            VALUES
                                (?, ?, 'MANAGER', 'ACTIVE', ?, 'Assigned from staff account management.', ?, NOW(6), NULL, NOW(6), NOW(6))
                            """,
                    propertyId,
                    accountId,
                    isPrimary,
                    currentUserId);
        }
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
