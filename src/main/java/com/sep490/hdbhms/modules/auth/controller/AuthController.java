package com.sep490.hdbhms.modules.auth.controller;

import com.sep490.hdbhms.modules.auth.dto.ChangePasswordRequest;
import com.sep490.hdbhms.modules.auth.dto.ChangePasswordResponse;
import com.sep490.hdbhms.modules.auth.dto.ForgotPasswordRequests;
import com.sep490.hdbhms.modules.auth.dto.ForgotPasswordResponses;
import com.sep490.hdbhms.modules.auth.dto.LoginRequest;
import com.sep490.hdbhms.modules.auth.dto.LoginResponse;
import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;
import com.sep490.hdbhms.modules.auth.service.AuthService;
import com.sep490.hdbhms.modules.auth.service.ForgotPasswordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authentication APIs for mobile clients")
public class AuthController {

    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;

    public AuthController(AuthService authService, ForgotPasswordService forgotPasswordService) {
        this.authService = authService;
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login by phone or email")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/change-password")
    @Operation(
            summary = "Change password for first-login onboarding",
            description = "After a successful first-login password change, mobile onboarding returns next_step=HOME. Identity verification is optional and no longer blocks mobile onboarding."
    )
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(authService.changePassword(currentUserId(jwt), request));
    }

    @GetMapping("/me/onboarding")
    @Operation(
            summary = "Get current onboarding state",
            description = "Mobile onboarding only requires first-login password change when must_change_password=true. Missing portrait or CCCD does not produce next_step=IDENTITY_VERIFICATION."
    )
    public ResponseEntity<OnboardingStateResponse> getOnboarding(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(authService.getOnboarding(currentUserId(jwt)));
    }

    @PostMapping("/forgot-password/request-otp")
    @Operation(summary = "Request forgot password OTP by registered email")
    public ResponseEntity<ForgotPasswordResponses.RequestOtp> requestForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequests.RequestOtp request
    ) {
        return ResponseEntity.ok(forgotPasswordService.requestOtp(request));
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Verify forgot password OTP and return reset token")
    public ResponseEntity<ForgotPasswordResponses.VerifyOtp> verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequests.VerifyOtp request
    ) {
        return ResponseEntity.ok(forgotPasswordService.verifyOtp(request));
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password with verified reset token")
    public ResponseEntity<ForgotPasswordResponses.ResetPassword> resetForgottenPassword(
            @Valid @RequestBody ForgotPasswordRequests.ResetPassword request
    ) {
        return ResponseEntity.ok(forgotPasswordService.resetPassword(request));
    }

    private Long currentUserId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
