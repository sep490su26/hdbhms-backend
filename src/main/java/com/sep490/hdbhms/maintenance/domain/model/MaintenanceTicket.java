package com.sep490.hdbhms.maintenance.domain.model;

import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTicket {
    Long id;
    String ticketCode;
    Long propertyId;
    Long roomId;
    Long contractId;
    Long createdById;
    TicketScope ticketScope;
    Priority priority;
    String category;
    String title;
    String description;
    MaintenanceTicketStatus status;
    String rejectionReason;
    Long assignedToId;
    String workerName;
    String repairItems;
    LocalDateTime completedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}