package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.repository;

import com.sep490.hdbhms.identityandaccess.application.port.out.PermissionRequestRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.PermissionRequest;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.PermissionRequestStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PermissionRequestEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPermissionRequestRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper.PermissionRequestPersistenceMapper;
import com.sep490.hdbhms.shared.specifications.PermissionRequestSpecifications;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataPermissionRequestRepository implements PermissionRequestRepository {
    JpaPermissionRequestRepository jpaPermissionRequestRepository;
    PermissionRequestPersistenceMapper permissionRequestPersistenceMapper;

    @Override
    public PermissionRequest save(PermissionRequest permissionRequest) {
        return permissionRequestPersistenceMapper.toDomain(
                jpaPermissionRequestRepository.save(
                        permissionRequestPersistenceMapper.toEntity(permissionRequest)
                )
        );
    }

    @Override
    public Optional<PermissionRequest> findById(Long id) {
        return jpaPermissionRequestRepository.findById(id)
                .map(permissionRequestPersistenceMapper::toDomain);
    }

    @Override
    public Page<PermissionRequest> findAll(PermissionRequestStatus status, Pageable pageable) {
        List<Long> ids = jpaPermissionRequestRepository.findAll().stream()
                .map(PermissionRequestEntity::getId)
                .toList();
        Specification<PermissionRequestEntity> specification = Specification
                .where(PermissionRequestSpecifications.idIn(ids))
                .and(PermissionRequestSpecifications.statusIn(status));
        return jpaPermissionRequestRepository.findAll(specification, pageable)
                .map(permissionRequestPersistenceMapper::toDomain);
    }
}
