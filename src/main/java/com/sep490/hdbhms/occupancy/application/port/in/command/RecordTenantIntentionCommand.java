package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record RecordTenantIntentionCommand(
        Long leaseContractId,
        String intention,
        LocalDate expectedMoveOutDate,
        String note
) {
}
