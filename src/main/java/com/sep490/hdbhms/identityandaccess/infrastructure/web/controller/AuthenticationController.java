package com.sep490.hdbhms.identityandaccess.infrastructure.web.controller;

import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request.*;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.AuthenticationResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.IntrospectResponse;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.mapper.AuthenticationWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    LoginUseCase loginUseCase;
    LogoutUseCase logoutUseCase;
    RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    ResetPasswordUseCase resetPasswordUseCase;
    IntrospectTokenUseCase introspectTokenUseCase;
    AuthenticationWebMapper authenticationWebMapper;


    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> login(
            @RequestHeader("X-Client-Type") String clientType,
            @RequestBody AuthenticationRequest authenticationRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(
                        authenticationWebMapper.toResponse(
                                loginUseCase.execute(
                                        clientType,
                                        authenticationWebMapper.toCommand(authenticationRequest),
                                        request,
                                        response
                                )
                        )
                )
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest introspectRequest) {
        return ApiResponse.<IntrospectResponse>builder()
                .data(
                        authenticationWebMapper.toResponse(
                                introspectTokenUseCase.execute(
                                        authenticationWebMapper.toCommand(introspectRequest)
                                )
                        )
                )
                .build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthenticationResponse> refreshToken(@RequestBody RefreshRequest refreshRequest, HttpServletRequest request, HttpServletResponse response) {
        return ApiResponse.<AuthenticationResponse>builder()
                .data(
                        authenticationWebMapper.toResponse(
                                refreshAccessTokenUseCase.execute(
                                        authenticationWebMapper.toCommand(refreshRequest),
                                        request,
                                        response
                                )
                        )
                )
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest logoutRequest, HttpServletRequest request, HttpServletResponse response) {
        logoutUseCase.execute(
                authenticationWebMapper.toCommand(logoutRequest),
                request,
                response
        );
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/forgot-password")
    ApiResponse<Void> forgotPassword(@RequestBody AccountPasswordResetRequest request) {
        resetPasswordUseCase.requestResetPassword(
                authenticationWebMapper.toCommand(request)
        );
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/reset-password")
    ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(
                authenticationWebMapper.toCommand(request)
        );
        return ApiResponse.<Void>builder().build();
    }
}
