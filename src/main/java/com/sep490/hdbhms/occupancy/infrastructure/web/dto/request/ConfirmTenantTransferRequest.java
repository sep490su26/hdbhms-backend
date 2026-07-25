package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;

public record ConfirmTenantTransferRequest(
        SettlementType settlementType,
        Long nominatedHolderProfileId
) {
}
