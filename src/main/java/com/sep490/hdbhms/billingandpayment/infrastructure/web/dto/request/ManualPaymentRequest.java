package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ManualPaymentRequest(
        @NotNull(message = "Vui lòng nhập số tiền nhận")
        @Positive(message = "Số tiền không hợp lệ") Long amount,
        String payerName,
        String note
) {
}
