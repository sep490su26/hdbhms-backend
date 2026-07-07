package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.repository;

import com.sep490.hdbhms.permissiongrant.application.port.out.PermissionAccessAuditLogRepository;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionAccessAuditLog;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.jpa.JpaPermissionAccessAuditLogRepository;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.mapper.PermissionAccessAuditLogPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPermissionAccessAuditLogRepository implements PermissionAccessAuditLogRepository {
    JpaPermissionAccessAuditLogRepository jpaRepository;
    PermissionAccessAuditLogPersistenceMapper mapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PermissionAccessAuditLog save(PermissionAccessAuditLog auditLog) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(auditLog)));
    }
}
