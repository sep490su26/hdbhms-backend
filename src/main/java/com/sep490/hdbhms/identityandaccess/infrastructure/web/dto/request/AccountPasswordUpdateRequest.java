package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountPasswordUpdateRequest {
    String newPassword;
    String currentPassword;
}