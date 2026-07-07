package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionRequestApprovalResponse {
    Long id;
    PermissionRequestStatus status;
    LocalDateTime decidedAt;
}
