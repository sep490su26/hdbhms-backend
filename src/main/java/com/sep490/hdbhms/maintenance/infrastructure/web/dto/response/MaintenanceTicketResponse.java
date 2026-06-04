package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
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
    @JsonProperty("ticket_scope")
    TicketScope ticketScope;
    Priority priority;
    String category;
    String title;
    String description;
    MaintenanceTicketStatus status;
    @JsonProperty("worker_name")
    String workerName;
    @JsonProperty("repair_items")
    String repairItems;
    @JsonProperty("root_cause")
    String rootCause;
    @JsonProperty("cost_amount")
    Long costAmount;
    @JsonProperty("cost_description")
    String costDescription;
    @JsonProperty("paid_by")
    PaidBy paidBy;
    @JsonProperty("completed_at")
    LocalDateTime completedAt;
    @JsonProperty("updated_at")
    LocalDateTime updatedAt;
    @JsonProperty("created_at")
    LocalDateTime createdAt;
}
