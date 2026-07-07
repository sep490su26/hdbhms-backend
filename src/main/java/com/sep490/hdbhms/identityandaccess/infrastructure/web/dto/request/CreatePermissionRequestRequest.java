package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.request;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePermissionRequestRequest {
    PermissionTargetType targetType;
    Long targetId;
}
