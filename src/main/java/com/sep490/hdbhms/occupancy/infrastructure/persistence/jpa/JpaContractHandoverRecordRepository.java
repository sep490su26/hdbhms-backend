package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.HandoverType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface JpaContractHandoverRecordRepository extends JpaRepository<ContractHandoverRecordEntity, Long> {
    Optional<ContractHandoverRecordEntity> findFirstByContract_IdAndHandoverTypeOrderByCreatedAtDesc(Long contractId, HandoverType handoverType);

    Optional<ContractHandoverRecordEntity> findByContract_IdAndHandoverType(Long contractId, HandoverType type);
}
