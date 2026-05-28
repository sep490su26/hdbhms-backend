package com.sep490.hdbhms.modules.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MaintenanceTicketRequests {

    private MaintenanceTicketRequests() {
    }

    public record CreateTicketRequest(
            @JsonProperty("room_id")
            Long roomId,
            String category,
            String title,
            String description,
            @JsonProperty("ticket_scope")
            String ticketScope,
            String priority,
            @JsonProperty("attachment_file_ids")
            List<Long> attachmentFileIds
    ) {
    }

    public record AcceptTicketRequest(String note) {
    }

    public record RejectTicketRequest(String reason) {
    }

    public record UpdateTicketProgressRequest(
            @JsonProperty("worker_name")
            String workerName,
            @JsonProperty("repair_items")
            String repairItems,
            @JsonProperty("expected_completion_date")
            LocalDate expectedCompletionDate,
            String note
    ) {
    }

    public record CompleteTicketRequest(
            @JsonProperty("completion_note")
            String completionNote,
            @JsonProperty("after_photo_file_ids")
            List<Long> afterPhotoFileIds,
            List<CostRequest> costs
    ) {
    }

    public record CostRequest(
            @JsonProperty("cost_type")
            String costType,
            String description,
            BigDecimal amount,
            @JsonProperty("paid_by")
            String paidBy
    ) {
    }

    public record ConfirmTicketRequest(
            @JsonProperty("satisfaction_note")
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
            @JsonProperty("satisfaction_note")
            String satisfactionNote
    ) {
    }
}
