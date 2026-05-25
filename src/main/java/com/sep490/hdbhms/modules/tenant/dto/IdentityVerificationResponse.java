package com.sep490.hdbhms.modules.tenant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;

public record IdentityVerificationResponse(
        boolean success,

        String message,

        @JsonProperty("portrait_file_id")
        Long portraitFileId,

        @JsonProperty("id_card_front_file_id")
        Long idCardFrontFileId,

        @JsonProperty("id_card_back_file_id")
        Long idCardBackFileId,

        @JsonProperty("profile_completed")
        boolean profileCompleted,

        @JsonProperty("identity_completed")
        boolean identityCompleted,

        OnboardingStateResponse onboarding
) {
}
