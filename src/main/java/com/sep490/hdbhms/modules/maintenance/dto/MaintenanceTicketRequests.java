package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MaintenanceTicketRequests {

    private MaintenanceTicketRequests() {
    }

    public record CreateTicketRequest(
            @JsonAlias("room_id")
            Long roomId,
            String category,
            String title,
            String description,
            @JsonAlias("ticket_scope")
            String ticketScope,
            String priority,
            @JsonAlias("attachment_file_ids")
            List<Long> attachmentFileIds
    ) {
    }

    public record AcceptTicketRequest(String note) {
    }

    public record RejectTicketRequest(String reason) {
    }

    public record UpdateTicketProgressRequest(
            @JsonAlias("worker_name")
            String workerName,
            @JsonAlias("repair_items")
            String repairItems,
            @JsonAlias("expected_completion_date")
            LocalDate expectedCompletionDate,
            String note
    ) {
    }

    public record CompleteTicketRequest(
            @JsonAlias("completion_note")
            String completionNote,
            @JsonAlias("after_photo_file_ids")
            List<Long> afterPhotoFileIds,
            List<CostRequest> costs
    ) {
    }

    public record CostRequest(
            @JsonAlias("cost_type")
            String costType,
            String description,
            BigDecimal amount,
            @JsonAlias("paid_by")
            String paidBy
    ) {
    }

    public record ConfirmTicketRequest(
            @JsonAlias("satisfaction_note")
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
            @JsonAlias("satisfaction_note")
            String satisfactionNote
    ) {
    }
}
