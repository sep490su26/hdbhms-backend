package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record AddCoOccupantToContractCommand(
        Long leaseContractId,
        Long tenantProfileId,
        LocalDate moveInDate,
        Long approvedBy
) {
}
