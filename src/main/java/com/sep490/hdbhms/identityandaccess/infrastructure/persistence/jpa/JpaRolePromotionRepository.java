package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.domain.value_objects.PromotionRole;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.RolePromotionStatus;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.RolePromotionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("""
            select rp
            from RolePromotionEntity rp
            join fetch rp.property p
            where rp.user.id = :userId
              and rp.role = :role
              and rp.status = :status
              and rp.deletedAt is null
              and rp.property is not null
            order by p.name asc
            """)
    List<RolePromotionEntity> findActiveAssignments(
            @Param("userId") Long userId,
            @Param("role") PromotionRole role,
            @Param("status") RolePromotionStatus status
    );

    @Query("""
            select distinct rp.user.id
            from RolePromotionEntity rp
            where rp.property.id = :propertyId
              and rp.role = :role
              and rp.status = :status
              and rp.deletedAt is null
              and rp.user.deletedAt is null
              and rp.user.status = :userStatus
            """)
    List<Long> findActiveUserIdsByPropertyId(
            @Param("propertyId") Long propertyId,
            @Param("role") PromotionRole role,
            @Param("status") RolePromotionStatus status,
            @Param("userStatus") AccountStatus userStatus
    );

    Optional<RolePromotionEntity> findFirstByUser_IdAndProperty_IdAndRoleAndDeletedAtIsNull(
            Long userId,
            Long propertyId,
            PromotionRole role
    );
}
