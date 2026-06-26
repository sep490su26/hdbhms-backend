package com.sep490.hdbhms.modules.tenant.dto;

import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;

public record IdentityVerificationResponse(
        boolean success,

        String message,
        Long portraitFileId,
        Long idCardFrontFileId,
        Long idCardBackFileId,
        boolean profileCompleted,
        boolean identityCompleted,

        OnboardingStatusResponse onboarding
) {
}
