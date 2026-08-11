package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.validator.FullName;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NotBlank(message = "Vui lòng nhập họ tên nhân viên.")
    @FullName(message = "Vui lòng nhập họ tên nhân viên.")
    String fullName;
    @Email(message = "Email không đúng định dạng")
    String email;
    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Size(min = 10, max = 10, message = "Số điện thoại phải dài đúng 10 ký tự")
    @VietnamesePhone(message = "Số điện thoại không đúng định dạng")
    String phone;
    @NotNull(message = "Vui lòng chọn vai trò")
    Role initialRole;
    Long propertyId;
}
