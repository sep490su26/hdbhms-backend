package com.sep490.hdbhms.accounting.infrastructure.web.dto.response;

import java.time.LocalDateTime;

public record ExpenseAttachmentResponse(
        Long id,
        Long fileId,
        String fileName,
        String attachmentType,
        LocalDateTime createdAt
) {
}
