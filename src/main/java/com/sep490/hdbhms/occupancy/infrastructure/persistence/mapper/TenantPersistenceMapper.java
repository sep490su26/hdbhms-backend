package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Tenant;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TenantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TenantPersistenceMapper {

    JpaUserRepository jpaUserRepository;
    JpaPropertyRepository jpaPropertyRepository;

    public Tenant toDomain(TenantEntity entity) {
        if (entity == null) return null;
        return Tenant.builder()
                .id(entity.getId())
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .activeTenantToken(entity.getActiveTenantToken())
                .build();
    }

    public TenantEntity toEntity(Tenant domain) {
        if (domain == null) return null;
        return TenantEntity.builder()
                .id(domain.getId())
                .user(domain.getUserId() != null
                        ? jpaUserRepository.findById(domain.getUserId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.TENANT_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .activeTenantToken(domain.getActiveTenantToken())
                .build();
    }
}
