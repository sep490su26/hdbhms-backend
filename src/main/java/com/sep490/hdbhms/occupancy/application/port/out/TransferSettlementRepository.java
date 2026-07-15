package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;

import java.util.Optional;

public interface TransferSettlementRepository {
    TransferSettlement save(TransferSettlement transferSettlement);

    Optional<TransferSettlement> findLatestByTransferRequestId(Long transferRequestId);

    Optional<TransferSettlement> findByTransferDifferenceInvoiceId(Long transferDifferenceInvoiceId);
}
