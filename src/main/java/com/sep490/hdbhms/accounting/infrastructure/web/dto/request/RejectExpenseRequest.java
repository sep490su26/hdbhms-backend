package com.sep490.hdbhms.accounting.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectExpenseRequest(
        @NotBlank(message = "Vui lòng nhập lý do từ chối")
        @Size(max = 2000, message = "Lý do từ chối không được vượt quá 2000 ký tự") String reason
) {
}
