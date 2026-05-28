package com.sep490.hdbhms.billingandpayment.infrastructure.persistence.mapper;

import com.sep490.hdbhms.billingandpayment.domain.model.RentOverride;
import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.RentOverrideEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.jpa.JpaUserRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RentOverridePersistenceMapper {
    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaUserRepository jpaUserRepository;

    public RentOverride toDomain(RentOverrideEntity entity) {
        if (entity == null) return null;
        return RentOverride.builder()
                .id(entity.getId())
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .billingPeriod(entity.getBillingPeriod())
                .overrideMonthlyRent(entity.getOverrideMonthlyRent())
                .reason(entity.getReason())
                .approvedBy(entity.getApprovedBy() != null ? entity.getApprovedBy().getId() : null)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RentOverrideEntity toEntity(RentOverride domain) {
        if (domain == null) return null;
        return RentOverrideEntity.builder()
                .id(domain.getId())
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.getReferenceById(domain.getContractId())
                        : null)
                .billingPeriod(domain.getBillingPeriod())
                .overrideMonthlyRent(domain.getOverrideMonthlyRent())
                .reason(domain.getReason())
                .approvedBy(domain.getApprovedBy() != null
                        ? jpaUserRepository.getReferenceById(domain.getApprovedBy())
                        : null)
                .createdAt(domain.getCreatedAt())
                .build();
    }
}