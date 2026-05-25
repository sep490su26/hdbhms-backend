package com.sep490.hdbhms.modules.tenant.repository;

import com.sep490.hdbhms.modules.tenant.entity.TenantMembership;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantMembershipRepository extends JpaRepository<TenantMembership, Long> {

    @Query(value = """
            SELECT t.id AS tenantId,
                   p.name AS tenantName,
                   u.role AS role,
                   t.property_id AS propertyId
            FROM tenants t
            JOIN users u ON u.id = t.user_id
            JOIN properties p ON p.id = t.property_id
            WHERE t.user_id = :userId
              AND t.deleted_at IS NULL
              AND u.deleted_at IS NULL
            UNION
            SELECT t.id AS tenantId,
                   p.name AS tenantName,
                   rp.role AS role,
                   rp.property_id AS propertyId
            FROM role_promotions rp
            JOIN properties p ON p.id = rp.property_id
            JOIN tenants t
                ON t.user_id = rp.user_id
               AND t.property_id = rp.property_id
               AND t.deleted_at IS NULL
            WHERE rp.user_id = :userId
              AND rp.status = 'ACTIVE'
              AND rp.deleted_at IS NULL
            """, nativeQuery = true)
    List<LoginTenantProjection> findActiveLoginTenants(@Param("userId") Long userId);

    interface LoginTenantProjection {
        Long getTenantId();

        String getTenantName();

        String getRole();

        Long getPropertyId();
    }
}
