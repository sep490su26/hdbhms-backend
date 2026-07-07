package com.sep490.hdbhms.permissiongrant.domain.model;

import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.permissiongrant.domain.valueObjects.PermissionAccessAction;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionAccessAuditLog {
    Long id;
    Long permissionGrantId;
    Long viewerUserId;
    TargetType targetType;
    Long targetId;
    PermissionAccessAction action;
    String reason;
    String ipAddress;
    String userAgent;
    LocalDateTime viewedAt;
    LocalDateTime createdAt;

    public static PermissionAccessAuditLog record(
            Long permissionGrantId,
            Long viewerUserId,
            TargetType targetType,
            Long targetId,
            PermissionAccessAction action,
            String ipAddress,
            String userAgent
    ) {
        return PermissionAccessAuditLog.builder()
                .permissionGrantId(permissionGrantId)
                .viewerUserId(viewerUserId)
                .targetType(targetType)
                .targetId(targetId)
                .action(action)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .viewedAt(LocalDateTime.now())
                .build();
    }
}
