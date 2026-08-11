package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountPasswordUpdateRequest {
    @NotBlank(message = "Vui lòng nhập mật khẩu mới.")
    @ValidPassword(message = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.")
    @Size(min = 8, message = "Độ dài mật khẩu không hợp lệ")
    String newPassword;
    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
    String currentPassword;
    @NotBlank(message = "Vui lòng xác nhận mật khẩu mới.")
    String confirmPassword;

    @AssertTrue(message = "Mật khẩu xác nhận chưa khớp.")
    public boolean isPasswordConfirmationMatching() {
        return newPassword == null || confirmPassword == null || newPassword.equals(confirmPassword);
    }
}
