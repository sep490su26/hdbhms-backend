package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionRequestResponse {
    Long id;
    Long requesterUserId;
    PermissionTargetType targetType;
    Long targetId;
    String rejectedReason;
    PermissionRequestStatus status;
    LocalDateTime expiresAt;
    LocalDateTime decidedAt;
    LocalDateTime createdAt;
}
