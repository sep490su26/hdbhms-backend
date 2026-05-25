package com.sep490.hdbhms.modules.mobile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MobileDepositContractResponse(
        Long id,

        @JsonProperty("deposit_code")
        String depositCode,

        String status,

        RoomDto room,

        BigDecimal amount,

        @JsonProperty("expected_move_in_date")
        LocalDate expectedMoveInDate,

        @JsonProperty("expected_lease_sign_date")
        LocalDate expectedLeaseSignDate,

        @JsonProperty("deposit_expires_at")
        LocalDate depositExpiresAt,

        @JsonProperty("created_at")
        LocalDate createdAt,

        String note,

        @JsonProperty("contract_file_url")
        String contractFileUrl
) {
    public record RoomDto(
            Long id,

            @JsonProperty("room_code")
            String roomCode,

            String name,

            @JsonProperty("area_m2")
            BigDecimal areaM2,

            @JsonProperty("image_url")
            String imageUrl
    ) {
    }
}
