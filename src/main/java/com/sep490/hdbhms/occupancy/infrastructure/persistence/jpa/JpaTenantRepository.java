package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTenantRepository extends JpaRepository<TenantEntity, Long> {
    boolean existsByUser_Email(String email);

    boolean existsByUser_Phone(String phone);

    Optional<TenantEntity> findByUser_Id(Long userId);
}
