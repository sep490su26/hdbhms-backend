package com.sep490.hdbhms.modules.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;

public record IdentityVerificationResponse(
        String message,

        @JsonProperty("identity_completed")
        boolean identityCompleted,

        OnboardingStateResponse onboarding
) {
}
