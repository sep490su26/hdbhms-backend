package com.sep490.hdbhms.booking.infrastructure.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositExtensionRequest(
        @NotNull(message = "Vui lòng nhập số ngày gia hạn.")
        @Min(value = 1, message = "Số ngày gia hạn tối thiểu là 1")
        @Max(value = 7, message = "Số ngày gia hạn tối đa là 7")
        Integer additionalDays,

        @NotBlank(message = "Vui lòng nhập lý do gia hạn.")
        String reason
) {
}
