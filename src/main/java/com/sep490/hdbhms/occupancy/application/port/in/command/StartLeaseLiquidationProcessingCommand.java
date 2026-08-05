package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;
import java.util.List;

public record StartLeaseLiquidationProcessingCommand(
        Long leaseContractId,
        LocalDate liquidationDate,
        String reason,
        String liquidationMode,
        List<Long> leavingProfileIds,
        List<Long> stayingProfileIds,
        Long replacementPrimaryTenantProfileId
) {
}
