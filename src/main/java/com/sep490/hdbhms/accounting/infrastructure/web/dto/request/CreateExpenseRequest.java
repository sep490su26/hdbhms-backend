package com.sep490.hdbhms.accounting.infrastructure.web.dto.request;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateExpenseRequest(
        @NotNull Long propertyId,
        Long roomId,
        @NotNull ExpenseType expenseType,
        @NotNull @Positive Long amount,
        @NotBlank @Size(max = 1000) String reason,
        @Size(max = 4000) String description,
        @Size(max = 255) String vendorName,
        LocalDate expectedPaymentDate,
        List<@Valid CreateExpenseAttachmentRequest> attachments
) {
}
