package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountEmailUpdateRequest {
    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Địa chỉ email sai định dạng")
    String newEmail;
    @NotBlank(message = "Vui lòng nhập mật khẩu cũ")
    String currentPassword;
}
