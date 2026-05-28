package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountFirstPasswordUpdateRequest {
    @NotBlank
    @Size(min = 8)
    String newPassword;
}