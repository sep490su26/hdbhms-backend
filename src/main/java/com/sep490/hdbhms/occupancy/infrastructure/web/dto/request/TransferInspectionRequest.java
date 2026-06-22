package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TransferInspectionRequest(
        @NotNull(message = "Transfer request ID is required")
        Long transferRequestId,
        
        List<AssetInspectionItem> assetInspections,
        String notes
) {
    public record AssetInspectionItem(
            Long assetId,
            String condition,
            Long compensationAmount,
            String description
    ) {}
}
