package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;
import jakarta.validation.constraints.NotNull;

public record ConfirmTenantTransferRequest(
        @NotNull SettlementType settlementType,
        Long nominatedHolderProfileId
) {
}