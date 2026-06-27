package com.sep490.hdbhms.modules.maintenance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MaintenanceTicketRequests {

    private MaintenanceTicketRequests() {
    }

    public record CreateTicketRequest(
            Long roomId,
            String category,
            String title,
            String description,
            String ticketScope,
            String priority,
            List<Long> attachmentFileIds
    ) {
    }

    public record AcceptTicketRequest(String note) {
    }

    public record RejectTicketRequest(String reason) {
    }

    public record UpdateTicketProgressRequest(
            String workerName,
            String repairItems,
            LocalDate expectedCompletionDate,
            String note
    ) {
    }

    public record CompleteTicketRequest(
            String completionNote,
            List<Long> afterPhotoFileIds,
            List<CostRequest> costs
    ) {
    }

    public record CostRequest(
            String costType,
            String description,
            BigDecimal amount,
            String paidBy
    ) {
    }

    public record ConfirmTicketRequest(
            String satisfactionNote
    ) {
    }

    public record ReviewTicketRequest(
            Integer rating,
            String comment
    ) {
    }

    public record ConfirmAndReviewRequest(
            Integer rating,
            String comment,
            String satisfactionNote
    ) {
    }
}
