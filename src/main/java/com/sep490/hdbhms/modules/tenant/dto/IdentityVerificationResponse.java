package com.sep490.hdbhms.modules.tenant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdentityVerificationResponse {
    boolean success;
    String message;
    Long portraitFileId;
    Long frontFileId;
    Long backFileId;
    boolean onboardingCompleted;
    boolean onBoardingCompleted;
    OnboardingStatusResponse onboarding;
}
