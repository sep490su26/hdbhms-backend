package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.mapper;

import com.sep490.hdbhms.identityandaccess.domain.model.EmergencyContact;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.EmergencyContactEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaPersonProfileRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmergencyContactPersistenceMapper {
    JpaPersonProfileRepository jpaPersonProfileRepository;

    public EmergencyContact toDomain(EmergencyContactEntity entity) {
        if (entity == null) return null;
        return EmergencyContact.builder()
                .id(entity.getId())
                .tenantProfileId(entity.getTenantProfile() != null ? entity.getTenantProfile().getId() : null)
                .fullName(entity.getFullName())
                .relationship(entity.getRelationship())
                .phone(entity.getPhone())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public EmergencyContactEntity toEntity(EmergencyContact domain) {
        if (domain == null) return null;
        return EmergencyContactEntity.builder()
                .id(domain.getId())
                .tenantProfile(domain.getTenantProfileId() != null
                        ? jpaPersonProfileRepository.findById(domain.getTenantProfileId())
                        .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .fullName(domain.getFullName())
                .relationship(domain.getRelationship())
                .phone(domain.getPhone())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
