package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountRoleUpdateRequest {
    Role role;
}