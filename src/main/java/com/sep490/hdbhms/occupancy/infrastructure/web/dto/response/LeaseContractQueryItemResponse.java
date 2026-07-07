package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaseContractQueryItemResponse(
        Long contractId,
        String contractCode,
        Long roomId,
        String roomCode,
        Long propertyId,
        String propertyName,
        String primaryTenantName,
        Integer occupantsCount,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate rentStartDate,
        Long monthlyRent,
        Integer paymentCycleMonths,
        LeaseStatus status,
        LocalDateTime signedAt,
        Long contractFileId,
        String contractFileName
) {
}
