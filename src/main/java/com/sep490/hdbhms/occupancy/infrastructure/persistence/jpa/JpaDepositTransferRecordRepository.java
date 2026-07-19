package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.DepositTransferRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaDepositTransferRecordRepository extends JpaRepository<DepositTransferRecordEntity, Long> {
    Optional<DepositTransferRecordEntity> findFirstByTransferRequest_IdOrderByIdDesc(Long transferRequestId);

    Optional<DepositTransferRecordEntity> findFirstByNewContract_IdOrderByIdDesc(Long newContractId);
}
