package com.sep490.hdbhms.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ForgotPasswordRequests {

    private ForgotPasswordRequests() {
    }

    public record RequestOtp(
            @NotBlank(message = "email không được rỗng")
            @Email(message = "email không hợp lệ")
            String email
    ) {
    }

    public record VerifyOtp(
            @NotBlank(message = "email không được rỗng")
            @Email(message = "email không hợp lệ")
            String email,

            @NotBlank(message = "otp không được rỗng")
            @Pattern(regexp = "\\d{6}", message = "otp phải gồm 6 chữ số")
            String otp
    ) {
    }

    public record ResetPassword(
            @NotBlank(message = "email không được rỗng")
            @Email(message = "email không hợp lệ")
            String email,

            @JsonProperty("reset_token")
            @NotBlank(message = "reset_token không được rỗng")
            String resetToken,

            @JsonProperty("new_password")
            @NotBlank(message = "new_password không được rỗng")
            @Size(min = 8, message = "Mật khẩu mới phải có ít nhất 8 ký tự")
            String newPassword,

            @JsonProperty("confirm_password")
            @NotBlank(message = "confirm_password không được rỗng")
            String confirmPassword
    ) {
    }
}
