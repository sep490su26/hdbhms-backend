package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.shared.validator.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountPasswordUpdateRequest {
    @NotBlank
    @ValidPassword
    String newPassword;
    @NotBlank
    @ValidPassword
    String currentPassword;
}