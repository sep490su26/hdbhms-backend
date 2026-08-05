package com.sep490.hdbhms.booking.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DepositForfeitureRequest(
        @NotBlank(message = "Vui lòng nhập lý do xử lý mất cọc.")
        String reason
) {
}
