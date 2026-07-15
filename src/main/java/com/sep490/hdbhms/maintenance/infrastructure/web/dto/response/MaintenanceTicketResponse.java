package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTicketResponse {
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
    String workerName;
    String repairmanName;
    String repairmanPhone;
    String repairItems;
    String rootCause;
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
    LocalDateTime updatedAt;
    LocalDateTime createdAt;

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
}
