package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.PermissionRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface JpaPermissionRequestRepository extends JpaRepository<PermissionRequestEntity, Long>, JpaSpecificationExecutor<PermissionRequestEntity> {
}
