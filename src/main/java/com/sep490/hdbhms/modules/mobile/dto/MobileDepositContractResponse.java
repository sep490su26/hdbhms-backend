package com.sep490.hdbhms.modules.mobile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MobileDepositContractResponse(
        Long id,
        String depositCode,

        String status,

        RoomDto room,

        BigDecimal amount,
        LocalDate expectedMoveInDate,
        LocalDate expectedLeaseSignDate,
        LocalDate depositExpiresAt,
        LocalDate createdAt,

        String note,
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
