package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TransferSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTransferSettlementRepository extends JpaRepository<TransferSettlementEntity, Long> {
    Optional<TransferSettlementEntity> findFirstByTransferRequest_NewContract_IdAndSettlementTypeAndTransferDifferenceInvoiceIsNullOrderByIdAsc(
            Long newContractId,
            SettlementType settlementType
    );
}
