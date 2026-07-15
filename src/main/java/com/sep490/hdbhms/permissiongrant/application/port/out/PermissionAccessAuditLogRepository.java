package com.sep490.hdbhms.permissiongrant.application.port.out;

import com.sep490.hdbhms.permissiongrant.domain.model.PermissionAccessAuditLog;

public interface PermissionAccessAuditLogRepository {
    PermissionAccessAuditLog save(PermissionAccessAuditLog auditLog);
}
