package com.sep490.hdbhms.accounting.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectExpenseRequest(
        @NotBlank @Size(max = 2000) String reason
) {
}
