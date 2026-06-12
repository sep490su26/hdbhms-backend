package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.RolePromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface JpaRolePromotionRepository extends JpaRepository<RolePromotionEntity, Long> {
    @Query("""
            select rp.property.id
            from RolePromotionEntity rp
            where rp.user.id = :userId
              and rp.role = :role
              and rp.status = :status
              and rp.deletedAt is null
              and rp.property is not null
            """)
    List<Long> findActivePropertyIds(
            @Param("userId") Long userId,
            @Param("role") PromotionRole role,
            @Param("status") RolePromotionStatus status
    );
}
