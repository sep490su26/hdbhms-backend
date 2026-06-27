package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractLiquidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaContractLiquidationRepository extends JpaRepository<ContractLiquidationEntity, Long> {
    Optional<ContractLiquidationEntity> findByContract_Id(Long contractId);
}
