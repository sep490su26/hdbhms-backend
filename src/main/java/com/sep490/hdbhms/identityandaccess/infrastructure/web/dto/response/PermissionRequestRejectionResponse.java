package com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PermissionRequestStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionRequestRejectionResponse {
    Long id;
    String rejectedReason;
    PermissionRequestStatus status;
    LocalDateTime decidedAt;
}
