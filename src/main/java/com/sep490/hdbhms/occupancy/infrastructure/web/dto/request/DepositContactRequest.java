package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.DepositContactOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DepositContactRequest(
        @NotNull(message = "Vui lòng chọn kết quả liên hệ.")
        DepositContactOutcome outcome,

        @NotBlank(message = "Vui lòng nhập ghi chú liên hệ.")
        String note
) {
}
