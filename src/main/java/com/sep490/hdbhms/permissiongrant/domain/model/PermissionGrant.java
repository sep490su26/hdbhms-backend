package com.sep490.hdbhms.permissiongrant.domain.model;

import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.permissiongrant.domain.value_objects.PermissionGrantDurationCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionGrant {
    Long id;
    Long granteeUserId;
    TargetType targetType;
    Long targetId;
    Long sourceChangeRequestId;
    Long grantedBy;
    String reason;
    PermissionGrantDurationCode durationCode;
    LocalDateTime grantedAt;
    LocalDateTime expiresAt;
    LocalDateTime revokedAt;
    Long revokedBy;
    String revokeReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static PermissionGrant tenantProfileGrant(
            Long granteeUserId,
            Long profileId,
            Long sourceChangeRequestId,
            Long grantedBy,
            String reason,
            PermissionGrantDurationCode durationCode
    ) {
        return accessGrant(
                granteeUserId,
                TargetType.TENANT_PROFILE,
                profileId,
                sourceChangeRequestId,
                grantedBy,
                reason,
                durationCode
        );
    }

    public static PermissionGrant accessGrant(
            Long granteeUserId,
            TargetType targetType,
            Long targetId,
            Long sourceChangeRequestId,
            Long grantedBy,
            String reason,
            PermissionGrantDurationCode durationCode
    ) {
        LocalDateTime now = LocalDateTime.now();
        return PermissionGrant.builder()
                .granteeUserId(granteeUserId)
                .targetType(targetType)
                .targetId(targetId)
                .sourceChangeRequestId(sourceChangeRequestId)
                .grantedBy(grantedBy)
                .reason(reason)
                .durationCode(durationCode)
                .grantedAt(now)
                .expiresAt(now.plus(durationCode.duration()))
                .build();
    }

    public void renew(Long sourceChangeRequestId, Long grantedBy, String reason, PermissionGrantDurationCode durationCode) {
        LocalDateTime now = LocalDateTime.now();
        this.sourceChangeRequestId = sourceChangeRequestId;
        this.grantedBy = grantedBy;
        this.reason = reason;
        this.durationCode = durationCode;
        this.grantedAt = now;
        this.expiresAt = now.plus(durationCode.duration());
    }

    public void revoke(Long revokedBy, String reason) {
        if (this.revokedAt != null) {
            return;
        }
        this.revokedAt = LocalDateTime.now();
        this.revokedBy = revokedBy;
        this.revokeReason = reason;
    }

    public boolean isActive(LocalDateTime now) {
        return revokedAt == null && expiresAt != null && expiresAt.isAfter(now);
    }
}
