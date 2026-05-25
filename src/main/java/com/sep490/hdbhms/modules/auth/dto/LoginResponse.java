package com.sep490.hdbhms.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record LoginResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        long expiresIn,

        UserInfo user,
        List<TenantInfo> tenants,
        OnboardingStateResponse onboarding
) {
    public record UserInfo(
            Long id,

            @JsonProperty("full_name")
            String fullName,

            String phone,
            String email,
            String status,

            @JsonProperty("must_change_password")
            boolean mustChangePassword,

            @JsonProperty("identity_completed")
            boolean identityCompleted
    ) {
    }

    public record TenantInfo(
            @JsonProperty("tenant_id")
            Long tenantId,

            @JsonProperty("tenant_name")
            String tenantName,

            String role,

            @JsonProperty("property_id")
            Long propertyId
    ) {
    }
}
