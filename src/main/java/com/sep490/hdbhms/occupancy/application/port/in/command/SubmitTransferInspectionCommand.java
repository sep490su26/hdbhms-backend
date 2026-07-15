package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.util.List;

public record SubmitTransferInspectionCommand(
        Long contractId,
        Long transferRequestId,
        Long inspectorId,
        List<AssetInspectionResult> assetInspections,
        String notes
) {
    public record AssetInspectionResult(
            Long assetId,
            String condition,
            Long compensationAmount,
            String description
    ) {}
}
