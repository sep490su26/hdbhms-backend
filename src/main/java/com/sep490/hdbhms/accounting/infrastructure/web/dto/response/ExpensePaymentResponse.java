package com.sep490.hdbhms.accounting.infrastructure.web.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpensePaymentResponse(
        Long id,
        LocalDate paymentDate,
        String paymentMethod,
        String paymentReference,
        Long receiptFileId,
        Long paidByUserId,
        LocalDateTime paidAt,
        String note
) {
}
