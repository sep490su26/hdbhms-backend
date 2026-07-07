package com.sep490.hdbhms.permissiongrant.infrastructure.persistence.repository;

import com.sep490.hdbhms.changerequest.domain.valueObjects.TargetType;
import com.sep490.hdbhms.permissiongrant.application.port.out.PermissionGrantRepository;
import com.sep490.hdbhms.permissiongrant.domain.model.PermissionGrant;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.jpa.JpaPermissionGrantRepository;
import com.sep490.hdbhms.permissiongrant.infrastructure.persistence.mapper.PermissionGrantPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPermissionGrantRepository implements PermissionGrantRepository {
    JpaPermissionGrantRepository jpaRepository;
    PermissionGrantPersistenceMapper mapper;

    @Override
    public PermissionGrant save(PermissionGrant grant) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(grant)));
    }

    @Override
    public Optional<PermissionGrant> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<PermissionGrant> findActive(Long granteeUserId, TargetType targetType, Long targetId, LocalDateTime now) {
        return jpaRepository
                .findFirstByGrantee_IdAndTargetTypeAndTargetIdAndRevokedAtIsNullAndExpiresAtAfterOrderByExpiresAtDescIdDesc(
                        granteeUserId,
                        targetType,
                        targetId,
                        now
                )
                .map(mapper::toDomain);
    }

    @Override
    public Optional<PermissionGrant> findLatest(Long granteeUserId, TargetType targetType, Long targetId) {
        return jpaRepository
                .findFirstByGrantee_IdAndTargetTypeAndTargetIdOrderByGrantedAtDescIdDesc(
                        granteeUserId,
                        targetType,
                        targetId
                )
                .map(mapper::toDomain);
    }

    @Override
    public List<PermissionGrant> findAllByTarget(TargetType targetType, Long targetId) {
        return jpaRepository.findAllByTargetTypeAndTargetIdOrderByGrantedAtDescIdDesc(targetType, targetId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
