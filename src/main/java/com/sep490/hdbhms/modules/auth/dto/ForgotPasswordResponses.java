package com.sep490.hdbhms.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class ForgotPasswordResponses {

    private ForgotPasswordResponses() {
    }

    public record RequestOtp(
            String message,

            @JsonProperty("expires_in_seconds")
            long expiresInSeconds
    ) {
    }

    public record VerifyOtp(
            @JsonProperty("reset_token")
            String resetToken,

            @JsonProperty("expires_in_seconds")
            long expiresInSeconds
    ) {
    }

    public record ResetPassword(
            String message
    ) {
    }
}
