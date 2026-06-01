package com.sep490.hdbhms.occupancy.infrastructure.persistence.mapper;

import com.sep490.hdbhms.occupancy.domain.model.ContractOccupant;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractOccupantEntity;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaLeaseContractRepository;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa.JpaTenantRepository;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ContractOccupantPersistenceMapper {

    JpaLeaseContractRepository jpaLeaseContractRepository;
    JpaTenantRepository jpaTenantRepository;

    public ContractOccupant toDomain(ContractOccupantEntity entity) {
        if (entity == null) return null;
        return ContractOccupant.builder()
                .id(entity.getId())
                .contractId(entity.getContract() != null ? entity.getContract().getId() : null)
                .tenantId(entity.getTenant() != null ? entity.getTenant().getId() : null)
                .occupantRole(entity.getOccupantRole())
                .moveInDate(entity.getMoveInDate())
                .moveOutDate(entity.getMoveOutDate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ContractOccupantEntity toEntity(ContractOccupant domain) {
        if (domain == null) return null;
        return ContractOccupantEntity.builder()
                .id(domain.getId())
                .contract(domain.getContractId() != null
                        ? jpaLeaseContractRepository.findById(domain.getContractId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .tenant(domain.getTenantId() != null
                        ? jpaTenantRepository.findById(domain.getTenantId())
                                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED))
                        : null)
                .occupantRole(domain.getOccupantRole())
                .moveInDate(domain.getMoveInDate())
                .moveOutDate(domain.getMoveOutDate())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
