package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.UtilityTariff;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.UtilityTariffEntity;
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
public class UtilityTariffPersistenceMapper {

    JpaPropertyRepository jpaPropertyRepository;
    JpaUserRepository jpaUserRepository;

    public UtilityTariff toDomain(UtilityTariffEntity entity) {
        if (entity == null) return null;
        return UtilityTariff.builder()
                .id(entity.getId())
                .propertyId(entity.getProperty() != null ? entity.getProperty().getId() : null)
                .utilityType(entity.getUtilityType())
                .unitPrice(entity.getUnitPrice())
                .freeAllowance(entity.getFreeAllowance())
                .serviceFeeWaiveElectricityThreshold(entity.getServiceFeeWaiveElectricityThreshold())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdById(entity.getCreatedBy() != null ? entity.getCreatedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public UtilityTariffEntity toEntity(UtilityTariff domain) {
        if (domain == null) return null;
        return UtilityTariffEntity.builder()
                .id(domain.getId())
                .property(domain.getPropertyId() != null
                        ? jpaPropertyRepository.findById(domain.getPropertyId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UTILITY_TARIFF_NOT_FOUND))
                        : null)
                .utilityType(domain.getUtilityType())
                .unitPrice(domain.getUnitPrice())
                .freeAllowance(domain.getFreeAllowance())
                .serviceFeeWaiveElectricityThreshold(domain.getServiceFeeWaiveElectricityThreshold())
                .effectiveFrom(domain.getEffectiveFrom())
                .effectiveTo(domain.getEffectiveTo())
                .createdBy(domain.getCreatedById() != null
                        ? jpaUserRepository.findById(domain.getCreatedById())
                                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND))
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
