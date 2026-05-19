package com.sep490.hdbhms.occupancy.infrastructure.persistence.repository;

import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTenantRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper.TenantPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataTenantRepository implements TenantRepository {
    JpaTenantRepository jpaTenantRepository;
    TenantPersistenceMapper tenantPersistenceMapper;

    @Override
    public Tenant save(Tenant tenant) {
        return tenantPersistenceMapper.toDomain(
                jpaTenantRepository.save(
                        tenantPersistenceMapper.toEntity(
                                tenant
                        )
                )
        );
    }

    @Override
    public Optional<Tenant> findById(Long id) {
        return jpaTenantRepository.findById(id)
                .map(tenantPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmailOrPhone(String email, String phone) {
        boolean emailResult = jpaTenantRepository.existsByUser_Email(email);
        return !emailResult && jpaTenantRepository.existsByUser_Phone(phone);
    }
}
