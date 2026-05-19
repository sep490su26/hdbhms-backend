package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.Tenant;

import java.util.Optional;

public interface TenantRepository {
    Tenant save(Tenant tenant);

    Optional<Tenant> findById(Long id);

    Optional<Tenant> findByUserId(Long userId);

    boolean existsByEmailOrPhone(String email, String phone);

    Optional<Tenant> findByUserIdAndPropertyId(Long id, Long propertyId);
}
