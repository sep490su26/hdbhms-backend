package com.sep490.hdbhms.accounting.infrastructure.web.dto.request;

import com.sep490.hdbhms.accounting.domain.value_objects.ExpenseAttachmentType;
import jakarta.validation.constraints.NotNull;

public record CreateExpenseAttachmentRequest(
        @NotNull Long fileId,
        ExpenseAttachmentType attachmentType
) {
}
