package com.sep490.hdbhms.modules.mobile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MobileLeaseContractResponse(
        Long id,
        String contractCode,

        String status,

        RoomDto room,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate rentStartDate,
        BigDecimal monthlyRent,
        Integer paymentCycleMonths,
        BigDecimal depositAmount,
        BigDecimal serviceFee,

        List<String> terms,
        String contractFileUrl
) {
    public record RoomDto(
            Long id,
            String roomCode,

            String name,
            BigDecimal areaM2,
            String imageUrl
    ) {
    }
}
