package com.sep490.hdbhms.permissiongrant.application.port.out;

import com.sep490.hdbhms.changerequest.domain.value_objects.TargetType;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PermissionGrantRepository {
    PermissionGrant save(PermissionGrant grant);
    Optional<PermissionGrant> findById(Long id);
    Optional<PermissionGrant> findActive(Long granteeUserId, TargetType targetType, Long targetId, LocalDateTime now);
    Optional<PermissionGrant> findLatest(Long granteeUserId, TargetType targetType, Long targetId);
    List<PermissionGrant> findAllByTarget(TargetType targetType, Long targetId);
}
