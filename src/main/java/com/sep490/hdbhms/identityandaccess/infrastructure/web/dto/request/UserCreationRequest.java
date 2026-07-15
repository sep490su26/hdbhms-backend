package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.shared.validator.FullName;
import com.sep490.hdbhms.shared.validator.VietnamesePhone;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserCreationRequest {
    @NotBlank
    @FullName
    String fullName;
    @Email
    String email;
    @NotBlank
    @VietnamesePhone
    String phone;
    @NotNull
    Role initialRole;
    Long propertyId;
}
