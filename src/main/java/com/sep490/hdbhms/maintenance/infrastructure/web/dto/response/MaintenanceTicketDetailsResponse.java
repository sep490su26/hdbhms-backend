package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.maintenance.domain.valueObjects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.valueObjects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.valueObjects.Priority;
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
    String ticketCode;
    Long propertyId;
    Long roomId;
    String roomCode;
    String roomName;
    String propertyName;
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
    String workerName;
    String repairmanName;
    String repairmanPhone;
    String repairItems;
    String rootCause;
    String rejectionReason;
    Long costAmount;
    Long actualCost;
    String costDescription;
    PaidBy paidBy;
    CostResponsibility costResponsibility;
    String ticketStatus;
    String ticketStatusLabel;
    String billingStatus;
    String billingStatusLabel;
    String billingPeriod;
    Long invoiceId;
    String invoiceCode;
    String invoiceStatus;
    String paymentStatus;
    Boolean chargeToTenant;
    String payer;
    String lineType;
    Long chargeAmount;
    String checkoutUrl;
    LocalDateTime completedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<AttachmentResponse> beforeAttachments;
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
        Long fileId;
        String url;
        String mimeType;
        String name;
        String phase;
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
        String fromStatus;
        String toStatus;
        String action;
        String note;
        UserSummary createdBy;
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
        LocalDateTime createdAt;
    }
}
