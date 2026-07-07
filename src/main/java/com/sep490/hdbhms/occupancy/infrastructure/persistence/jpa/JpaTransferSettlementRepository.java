package com.sep490.hdbhms.occupancy.infrastructure.persistence.jpa;

import com.sep490.hdbhms.occupancy.domain.valueObjects.SettlementType;
import com.sep490.hdbhms.occupancy.infrastructure.persistence.entity.TransferSettlementEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaTransferSettlementRepository extends JpaRepository<TransferSettlementEntity, Long> {
    Optional<TransferSettlementEntity> findFirstByTransferRequest_NewContract_IdAndSettlementTypeAndTransferDifferenceInvoiceIsNullOrderByIdAsc(
            Long newContractId,
            SettlementType settlementType
    );

    Optional<TransferSettlementEntity> findFirstByTransferRequest_IdOrderByIdDesc(Long transferRequestId);

    Optional<TransferSettlementEntity> findFirstByTransferDifferenceInvoice_IdOrderByIdDesc(Long transferDifferenceInvoiceId);
}
