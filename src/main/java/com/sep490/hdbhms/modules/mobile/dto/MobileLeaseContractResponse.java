package com.sep490.hdbhms.modules.mobile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MobileLeaseContractResponse(
        Long id,

        @JsonProperty("contract_code")
        String contractCode,

        String status,

        RoomDto room,

        @JsonProperty("start_date")
        LocalDate startDate,

        @JsonProperty("end_date")
        LocalDate endDate,

        @JsonProperty("rent_start_date")
        LocalDate rentStartDate,

        @JsonProperty("monthly_rent")
        BigDecimal monthlyRent,

        @JsonProperty("payment_cycle_months")
        Integer paymentCycleMonths,

        @JsonProperty("deposit_amount")
        BigDecimal depositAmount,

        @JsonProperty("service_fee")
        BigDecimal serviceFee,

        List<String> terms,

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
