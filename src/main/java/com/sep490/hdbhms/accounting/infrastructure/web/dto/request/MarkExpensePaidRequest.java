package com.sep490.hdbhms.accounting.infrastructure.web.dto.request;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpensePaymentMethod;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MarkExpensePaidRequest(
        LocalDate paymentDate,
        ExpensePaymentMethod paymentMethod,
        @Size(max = 100) String paymentReference,
        Long receiptFileId,
        @Size(max = 2000) String note
) {
}
