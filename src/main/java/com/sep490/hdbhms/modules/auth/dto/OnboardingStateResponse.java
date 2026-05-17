package com.sep490.hdbhms.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OnboardingStateResponse(
        @JsonProperty("user_id")
        Long userId,

        @JsonProperty("must_change_password")
        boolean mustChangePassword,

        @JsonProperty("identity_completed")
        boolean identityCompleted,

        @JsonProperty("next_step")
        String nextStep
) {
}
