package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;

public record UpdateLeaseContractTermsCommand(
        Long leaseContractId,
        LocalDate startDate,
        LocalDate endDate,
        Integer paymentCycleMonths,
        Long monthlyRent,
        Long depositAmount
) {
}
