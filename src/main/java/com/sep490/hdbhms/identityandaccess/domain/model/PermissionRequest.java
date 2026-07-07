package com.sep490.hdbhms.identityandaccess.domain.model;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionTargetType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionRequest {
    Long id;
    Long requesterUserId;
    PermissionTargetType targetType;
    Long targetId;
    String rejectedReason;
    PermissionRequestStatus status;
    LocalDateTime expiresAt;
    LocalDateTime decidedAt;
    LocalDateTime createdAt;

    public static PermissionRequest submit(
            Long requesterUserId,
            PermissionTargetType targetType,
            Long targetId
    ) {
        return PermissionRequest.builder()
                .requesterUserId(requesterUserId)
                .targetType(targetType)
                .targetId(targetId)
                .status(PermissionRequestStatus.PENDING)
                .build();
    }

    public void approve() {
        this.status = PermissionRequestStatus.APPROVED;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = PermissionRequestStatus.REJECTED;
        this.rejectedReason = reason;
        this.decidedAt = LocalDateTime.now();
    }

    public void revoke() {
        this.status = PermissionRequestStatus.REVOKED;
    }

    public void expire() {
        this.status = PermissionRequestStatus.EXPIRED;
    }
}
