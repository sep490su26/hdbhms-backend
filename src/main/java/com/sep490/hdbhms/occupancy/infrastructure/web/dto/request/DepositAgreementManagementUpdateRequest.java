package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DepositAgreementManagementUpdateRequest(
        @JsonProperty("depositorPhone")
        @JsonAlias({"depositor_phone", "phone"})
        @NotBlank(message = "Số điện thoại không được để trống")
        String depositorPhone,

        @JsonProperty("permanentAddress")
        @JsonAlias({"permanent_address", "address"})
        @NotBlank(message = "Địa chỉ không được để trống")
        String permanentAddress,

        @JsonProperty("expectedLeaseSignDate")
        @JsonAlias("expected_lease_sign_date")
        @NotNull(message = "Ngày ký hợp đồng dự kiến không được để trống")
        LocalDate expectedLeaseSignDate,

        @JsonProperty("expectedMoveInDate")
        @JsonAlias("expected_move_in_date")
        @NotNull(message = "Ngày vào ở dự kiến không được để trống")
        LocalDate expectedMoveInDate
) {
}
