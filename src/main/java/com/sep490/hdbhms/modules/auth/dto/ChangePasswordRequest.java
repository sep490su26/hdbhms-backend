package com.sep490.hdbhms.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @JsonProperty("new_password")
        @NotBlank(message = "new_password khong duoc rong")
        String newPassword,

        @JsonProperty("confirm_password")
        @NotBlank(message = "confirm_password khong duoc rong")
        String confirmPassword
) {
}
