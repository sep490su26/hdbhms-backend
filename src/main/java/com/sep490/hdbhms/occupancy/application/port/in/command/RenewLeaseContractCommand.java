package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record RenewLeaseContractCommand(
        Long leaseContractId,
        LocalDate newStartDate,
        LocalDate newEndDate,
        Long monthlyRent,
        Integer paymentCycleMonths,
        Long depositAmount,
        String newContractCode,
        String note
) {
}
