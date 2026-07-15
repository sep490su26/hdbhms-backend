package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositExtensionRequest(
        @NotNull(message = "Vui lòng nhập số ngày gia hạn.")
        @Min(value = 1, message = "Thời gian gia hạn tối thiểu là 1 ngày.")
        @Max(value = 7, message = "Chỉ được gia hạn tối đa 7 ngày.")
        Integer additionalDays,

        @NotBlank(message = "Vui lòng nhập lý do gia hạn.")
        String reason
) {
}
