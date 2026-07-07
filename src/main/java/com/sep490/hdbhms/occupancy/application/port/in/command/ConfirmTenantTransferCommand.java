package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.occupancy.domain.valueObjects.SettlementType;

public record ConfirmTenantTransferCommand(
        Long requestId,
        Long tenantId,
        SettlementType settlementType,
        Long nominatedHolderProfileId
) {
}