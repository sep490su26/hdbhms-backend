package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTicketDetailsResponse {
    Long id;
    @JsonProperty("ticket_code")
    String ticketCode;
    @JsonProperty("property_id")
    Long propertyId;
    @JsonProperty("room_id")
    Long roomId;
    @JsonProperty("room_code")
    String roomCode;
    @JsonProperty("room_name")
    String roomName;
    @JsonProperty("property_name")
    String propertyName;
    @JsonProperty("ticket_scope")
    String ticketScope;
    String scope;
    Priority priority;
    Priority severity;
    String category;
    String title;
    String description;
    String status;
    UserSummary createdBy;
    UserSummary assignedTo;
    @JsonProperty("worker_name")
    String workerName;
    @JsonProperty("repairman_name")
    String repairmanName;
    @JsonProperty("repairman_phone")
    String repairmanPhone;
    @JsonProperty("repair_items")
    String repairItems;
    @JsonProperty("root_cause")
    String rootCause;
    String rejectionReason;
    @JsonProperty("cost_amount")
    Long costAmount;
    @JsonProperty("actual_cost")
    Long actualCost;
    @JsonProperty("cost_description")
    String costDescription;
    @JsonProperty("paid_by")
    PaidBy paidBy;
    @JsonProperty("cost_responsibility")
    CostResponsibility costResponsibility;
    String ticketStatus;
    String ticketStatusLabel;
    String billingStatus;
    String billingStatusLabel;
    String billingPeriod;
    Long invoiceId;
    String invoiceCode;
    String invoiceStatus;
    String lineType;
    Long chargeAmount;
    String checkoutUrl;
    @JsonProperty("completed_at")
    LocalDateTime completedAt;
    @JsonProperty("created_at")
    LocalDateTime createdAt;
    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
    @JsonProperty("before_attachments")
    List<AttachmentResponse> beforeAttachments;
    @JsonProperty("after_attachments")
    List<AttachmentResponse> afterAttachments;
    List<AttachmentResponse> attachments;
    List<EventResponse> events;
    ReviewResponse review;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserSummary {
        Long id;
        String email;
        String phone;
        String role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttachmentResponse {
        Long id;
        @JsonProperty("file_id")
        Long fileId;
        String url;
        @JsonProperty("mime_type")
        String mimeType;
        String name;
        String phase;
        @JsonProperty("sort_order")
        Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class EventResponse {
        Long id;
        @JsonProperty("from_status")
        String fromStatus;
        @JsonProperty("to_status")
        String toStatus;
        String action;
        String note;
        UserSummary createdBy;
        @JsonProperty("created_at")
        LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ReviewResponse {
        Long id;
        Integer rating;
        String comment;
        UserSummary reviewer;
        @JsonProperty("created_at")
        LocalDateTime createdAt;
    }
}
