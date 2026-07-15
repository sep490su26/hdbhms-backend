package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.jpa;

import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity.PermissionAccessAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPermissionAccessAuditLogRepository extends JpaRepository<PermissionAccessAuditLogEntity, Long> {
}
