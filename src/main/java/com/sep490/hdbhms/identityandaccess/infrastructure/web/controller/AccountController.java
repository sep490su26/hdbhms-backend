package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountByIdQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountLoginHistoryQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountsQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.AccountResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.LoginHistoryResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.AccountWebMapper;
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
@RequestMapping("/api/v1/accounts")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {
    AccountWebMapper accountWebMapper;
    GetAccountUseCase getAccountUseCase;
    GetAccountsUseCase getAccountsUseCase;
    CreateAccountUseCase createAccountUseCase;
    UpdateAccountUseCase updateAccountUseCase;
    GetAccountLoginHistoryListUseCase getAccountLoginHistoryListUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_WRITE')")
    ApiResponse<AccountResponse> createAccount(@Valid @RequestBody AccountCreationRequest request) {
        return ApiResponse.<AccountResponse>builder()
                .data(
                        accountWebMapper.toAccountResponse(
                                createAccountUseCase.execute(accountWebMapper.toCommand(request))
                        )
                )
                .build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    ApiResponse<PageResponse<AccountResponse>> getAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(required = false) List<String> statuses,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        var command = new GetAccountsQuery(
                keyword,
                roles,
                statuses,
                pageable
        );
        return ApiResponse.<PageResponse<AccountResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getAccountsUseCase.execute(command)
                                        .map(accountWebMapper::toAccountResponse)
                        )
                )
                .build();
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    ApiResponse<AccountResponse> getAccount(@PathVariable Long accountId) {
        return ApiResponse.<AccountResponse>builder()
                .data(
                        accountWebMapper.toAccountResponse(
                                getAccountUseCase.getById(new GetAccountByIdQuery(accountId))
                        )
                )
                .build();
    }

//
//    @PatchMapping(value = "/me/email")
//    ApiResponse<Void> updateMyEmail(@Valid @RequestBody AccountEmailUpdateRequest emailUpdateRequest, HttpServletRequest request, HttpServletResponse response) {
//        var username = AuthUtils.getUsernameFromToken();
//        var account = getAccountUseCase.getByUsername(new GetAccountByUsernameQuery(username));
//        updateAccountUseCase.requestUpdateAccountEmail(new UpdateAccountEmailCommand(
//                account.getId(),
//                emailUpdateRequest.getNewEmail(),
//                emailUpdateRequest.getCurrentPassword(),
//                request,
//                response
//        ));
//        return ApiResponse.<Void>builder()
//                .build();
//    }
//
//    @PostMapping(value = "/me/email/confirm")
//    ApiResponse<AccountResponse> confirmMyEmailUpdate(@Valid @RequestBody AccountEmailUpdateConfirmationRequest emailUpdateConfirmationRequest, HttpServletRequest request, HttpServletResponse response) {
//        var username = AuthUtils.getUsernameFromToken();
//        var account = getAccountUseCase.getByUsername(new GetAccountByUsernameQuery(username));
//        return ApiResponse.<AccountResponse>builder()
//                .data(
//                        accountWebMapper.toAccountResponse(
//                                updateAccountUseCase.confirmUpdateAccountEmail(new VerifyUpdateEmailCommand(
//                                        account,
//                                        emailUpdateConfirmationRequest.getOtpCode(),
//                                        request,
//                                        response
//                                ))
//                        )
//                )
//                .build();
//    }
//
//    @PatchMapping(value = "/me/password")
//    ApiResponse<AccountResponse> updateMyPassword(@Valid @RequestBody AccountPasswordUpdateRequest usernameUpdateRequest, HttpServletRequest request, HttpServletResponse response) {
//        var username = AuthUtils.getUsernameFromToken();
//        var account = getAccountUseCase.getByUsername(new GetAccountByUsernameQuery(username));
//        return ApiResponse.<AccountResponse>builder()
//                .data(accountWebMapper.toAccountResponse(
//                        updateAccountUseCase.updateAccountPassword(new UpdateAccountPasswordCommand(
//                                account,
//                                usernameUpdateRequest.getCurrentPassword(),
//                                usernameUpdateRequest.getNewPassword(),
//                                request,
//                                response
//                        ))
//                ))
//                .build();
//    }
//
//    @PreAuthorize("hasAuthority('ACCOUNT_WRITE')")
//    @PutMapping(value = "/{accountId}/status")
//    ApiResponse<AccountResponse> updateAccountStatus(
//            @PathVariable Long accountId,
//            @Valid @RequestBody AccountStatusUpdateRequest request
//    ) {
//        return ApiResponse.<AccountResponse>builder()
//                .data(
//                        accountWebMapper.toAccountResponse(
//                                updateAccountUseCase.updateAccountStatus(
//                                        new UpdateAccountStatusCommand(
//                                                accountId,
//                                                request.getStatus()
//                                        )
//                                )
//                        )
//                )
//                .build();
//    }
//
//    @PreAuthorize("hasAuthority('ACCOUNT_WRITE')")
//    @PutMapping(value = "/{accountId}/role")
//    ApiResponse<AccountResponse> updateAccountRole(
//            @PathVariable Long accountId,
//            @Valid @RequestBody AccountRoleUpdateRequest request
//    ) {
//        return ApiResponse.<AccountResponse>builder()
//                .data(
//                        accountWebMapper.toAccountResponse(
//                                updateAccountUseCase.updateAccountRole(
//                                        new UpdateAccountRoleCommand(
//                                                accountId,
//                                                request.getRoleName()
//                                        )
//                                )
//                        )
//                )
//                .build();
//    }

    @PreAuthorize("hasAuthority('ACCOUNT_READ')")
    @GetMapping("/{accountId}/login-history")
    ApiResponse<PageResponse<LoginHistoryResponse>> getLoginHistory(
            @PathVariable Long accountId,
            @RequestParam(required = false) List<String> statuses,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.<PageResponse<LoginHistoryResponse>>builder()
                .data(
                        PageResponse.fromPageToPageResponse(
                                getAccountLoginHistoryListUseCase.execute(
                                                new GetAccountLoginHistoryQuery(
                                                        accountId,
                                                        statuses,
                                                        pageable
                                                ))
                                        .map(accountWebMapper::toLoginHistoryResponse)
                        )
                )
                .build();
    }
}
