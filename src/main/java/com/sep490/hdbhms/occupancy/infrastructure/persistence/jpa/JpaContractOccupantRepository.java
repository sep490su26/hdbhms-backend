package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.OccupantStatus;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractOccupantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaContractOccupantRepository extends JpaRepository<ContractOccupantEntity, Long> {
    Optional<ContractOccupantEntity> findFirstByContract_IdAndTenantProfile_IdAndStatus(
            Long contractId,
            Long tenantProfileId,
            OccupantStatus status
    );
}
