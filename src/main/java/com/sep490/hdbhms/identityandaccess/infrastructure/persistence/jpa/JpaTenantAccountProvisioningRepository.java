package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.TenantAccountProvisioningEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface JpaTenantAccountProvisioningRepository
        extends JpaRepository<TenantAccountProvisioningEntity, Long> {

    Optional<TenantAccountProvisioningEntity> findByTenantProfileId(Long tenantProfileId);

    List<TenantAccountProvisioningEntity> findAllByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT provisioning
            FROM TenantAccountProvisioningEntity provisioning
            WHERE provisioning.tenantProfileId = :tenantProfileId
            """)
    Optional<TenantAccountProvisioningEntity> findByTenantProfileIdForUpdate(
            @Param("tenantProfileId") Long tenantProfileId
    );
}
