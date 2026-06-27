package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DepositAgreementManagementUpdateRequest(
        @NotBlank(message = "Số điện thoại không được để trống")
        String depositorPhone,

        @NotBlank(message = "Địa chỉ không được để trống")
        String permanentAddress,

        @NotNull(message = "Ngày ký hợp đồng dự kiến không được để trống")
        LocalDate expectedLeaseSignDate,

        @NotNull(message = "Ngày vào ở dự kiến không được để trống")
        LocalDate expectedMoveInDate
) {
}
