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
public class AccountPasswordResetRequest {
    @NotBlank(message = "Vui lòng nhập địa chỉ email.")
    @Email(message = "Vui lòng nhập địa chỉ email hợp lệ.")
    String email;
}
