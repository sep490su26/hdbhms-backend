package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.PropertyRule;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.PropertyRuleEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaPropertyRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyRulePersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;

    public PropertyRule toDomain(PropertyRuleEntity entity) {
        if (entity == null) return null;
        return PropertyRule.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .ruleCode(entity.getRuleCode())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .defaultFineAmount(entity.getDefaultFineAmount())
                .sortOrder(entity.getSortOrder())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public PropertyRuleEntity toEntity(PropertyRule domain) {
        if (domain == null) return null;
        return PropertyRuleEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .ruleCode(domain.getRuleCode())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .defaultFineAmount(domain.getDefaultFineAmount())
                .sortOrder(domain.getSortOrder())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
