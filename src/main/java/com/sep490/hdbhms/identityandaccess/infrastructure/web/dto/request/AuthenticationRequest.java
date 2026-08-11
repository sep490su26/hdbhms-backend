package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationRequest {
    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(regexp = "^0\\d{9}$", message = "Vui lòng nhập số điện thoại 10 chữ số")
    String phone;
    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @ValidPassword(message = "Mật khẩu phải có ít nhất 8 ký tự, gồm chữ và số.")
    String password;
}
