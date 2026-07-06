package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.jpa;

import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.entity.PermissionGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaPermissionGrantRepository extends JpaRepository<PermissionGrantEntity, Long> {
    Optional<PermissionGrantEntity> findFirstByGrantee_IdAndTargetTypeAndTargetIdAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDescIdDesc(
            Long granteeUserId,
            TargetType targetType,
            Long targetId,
            LocalDateTime now
    );

    Optional<PermissionGrantEntity> findFirstByGrantee_IdAndTargetTypeAndTargetIdOrderByGrantedAtDescIdDesc(
            Long granteeUserId,
            TargetType targetType,
            Long targetId
    );

    List<PermissionGrantEntity> findAllByTargetTypeAndTargetIdOrderByGrantedAtDescIdDesc(
            TargetType targetType,
            Long targetId
    );
}
