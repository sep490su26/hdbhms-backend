package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.occupancy.domain.model.TransferSettlement;

public interface TransferSettlementRepository {
    TransferSettlement save(TransferSettlement transferSettlement);
}
