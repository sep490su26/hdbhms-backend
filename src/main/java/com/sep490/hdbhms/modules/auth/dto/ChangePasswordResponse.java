package com.sep490.hdbhms.modules.auth.dto;

public record ChangePasswordResponse(
        String message,
        OnboardingStateResponse onboarding
) {
}
