package com.sep490.hdbhms.accounting.infrastructure.web.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseRequestResponse(
        Long id,
        String expenseCode,
        String expenseType,
        String status,
        String approvalStatus,
        String paymentStatus,
        Long propertyId,
        String propertyName,
        Long roomId,
        String roomCode,
        Long amount,
        String description,
        String reason,
        String vendorName,
        LocalDate expectedPaymentDate,
        LocalDate expenseDate,
        Long changeRequestId,
        String requestCode,
        Long requesterId,
        Long approvedByUserId,
        LocalDateTime approvedAt,
        ExpensePaymentResponse payment,
        List<ExpenseAttachmentResponse> attachments,
        List<ExpenseTimelineResponse> timeline,
        LocalDateTime createdAt
) {
}
