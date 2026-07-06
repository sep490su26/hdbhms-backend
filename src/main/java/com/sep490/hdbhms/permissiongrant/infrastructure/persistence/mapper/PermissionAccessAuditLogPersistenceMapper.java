package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionAccessAuditLog;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity.PermissionAccessAuditLogEntity;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity.PermissionGrantEntity;
import jakarta.persistence.EntityManager;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PermissionAccessAuditLogPersistenceMapper {
    EntityManager entityManager;

    public PermissionAccessAuditLog toDomain(PermissionAccessAuditLogEntity entity) {
        if (entity == null) return null;
        return PermissionAccessAuditLog.builder()
                .id(entity.getId())
                .permissionGrantId(entity.getPermissionGrant() == null ? null : entity.getPermissionGrant().getId())
                .viewerUserId(entity.getViewer() == null ? null : entity.getViewer().getId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .action(entity.getAction())
                .reason(entity.getReason())
                .ipAddress(entity.getIpAddress())
                .userAgent(entity.getUserAgent())
                .viewedAt(entity.getViewedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public PermissionAccessAuditLogEntity toEntity(PermissionAccessAuditLog domain) {
        if (domain == null) return null;
        return PermissionAccessAuditLogEntity.builder()
                .id(domain.getId())
                .permissionGrant(permissionGrantReference(domain.getPermissionGrantId()))
                .viewer(userReference(domain.getViewerUserId()))
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .action(domain.getAction())
                .reason(domain.getReason())
                .ipAddress(domain.getIpAddress())
                .userAgent(domain.getUserAgent())
                .viewedAt(domain.getViewedAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private PermissionGrantEntity permissionGrantReference(Long id) {
        return id == null ? null : entityManager.getReference(PermissionGrantEntity.class, id);
    }

    private UserEntity userReference(Long id) {
        return id == null ? null : entityManager.getReference(UserEntity.class, id);
    }
}
