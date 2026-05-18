package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.ContractHandoverRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaContractHandoverRecordRepository extends JpaRepository<ContractHandoverRecordEntity, Long> {
}
