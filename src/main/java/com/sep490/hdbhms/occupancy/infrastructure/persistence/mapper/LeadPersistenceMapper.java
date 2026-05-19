package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Lead;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.LeadEntity;
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
public class LeadPersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;
    JpaUserRepository jpaUserRepository;

    public Lead toDomain(LeadEntity entity) {
        if (entity == null) return null;
        return Lead.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .desiredMoveInDate(entity.getDesiredMoveInDate())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public LeadEntity toEntity(Lead domain) {
        if (domain == null) return null;
        return LeadEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .user(domain.getUserId() != null
                        ? jpaUserRepository.findById(domain.getUserId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .desiredMoveInDate(domain.getDesiredMoveInDate())
                .note(domain.getNote())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
