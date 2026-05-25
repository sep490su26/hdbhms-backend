package com.sep490.hdbhms.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @JsonProperty("phone_or_email")
        @NotBlank(message = "phone_or_email không được rỗng")
        String phoneOrEmail,

        @NotBlank(message = "password không được rỗng")
        String password
) {
}
