package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record StartLeaseLiquidationProcessingCommand(
        Long leaseContractId,
        LocalDate liquidationDate,
        String reason
) {
}
