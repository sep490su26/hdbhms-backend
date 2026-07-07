package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa;

import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
    @Query(value = "SELECT user_id FROM hdbhms.users WHERE MATCH(email) AGAINST (CONCAT(?1, '*') IN BOOLEAN MODE)",
            nativeQuery = true)
    List<Long> findIdsByFullText(String keyword);

    @Query("""
            SELECT u.id
            FROM UserEntity u
            WHERE u.deletedAt IS NULL
              AND u.status = :status
              AND u.role IN :roles
            """)
    List<Long> findIdsByRolesAndStatus(
            @Param("roles") Collection<Role> roles,
            @Param("status") AccountStatus status
    );


    boolean existsByEmailAndDeletedAtIsNull(String email);

    boolean existsByPhoneAndDeletedAtIsNull(String phone);

    Optional<UserEntity> findByPhoneOrEmailAndDeletedAtIsNull(String phone, String email);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByPhone(String phone);

    boolean existsByRole(Role role);

    Optional<UserEntity> findByRole(Role role);
}
