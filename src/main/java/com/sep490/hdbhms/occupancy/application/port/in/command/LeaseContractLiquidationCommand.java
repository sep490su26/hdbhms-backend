package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;
import java.util.List;

public record LeaseContractLiquidationCommand(
        Long leaseContractId,
        LocalDate liquidationDate,
        String reason,
        List<LiquidationChargeInput> charges
) {
}
