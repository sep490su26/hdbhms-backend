package com.sep490.hdbhms.maintenance.domain.model;

import com.sep490.hdbhms.maintenance.domain.valueObjects.MaintenanceTicketStatus;
import com.sep490.hdbhms.maintenance.domain.valueObjects.Priority;
import com.sep490.hdbhms.maintenance.domain.valueObjects.TicketScope;
import com.sep490.hdbhms.shared.utils.StringUtils;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
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
    @Builder.Default
    Priority priority = Priority.URGENT;
    @Builder.Default
    String category = "";
    @Builder.Default
    String title = "";
    String description;

    @Builder.Default
    MaintenanceTicketStatus status = MaintenanceTicketStatus.PENDING_ACCEPTANCE;
    String rejectionReason;
    Long assignedToId;
    String workerName;
    String externalRepairmanName;
    String externalRepairmanPhone;
    String externalRepairProvider;
    String externalRepairNote;
    String repairmanPhone;
    String repairItems;
    LocalDateTime completedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public static MaintenanceTicket newMaintenanceTicket(
            Long propertyId,
            Long roomId,
            Long contractId,
            Long createdById,
            TicketScope ticketScope,
            String description
    ) {
        return MaintenanceTicket.builder()
                .propertyId(propertyId)
                .roomId(roomId)
                .contractId(contractId)
                .createdById(createdById)
                .ticketScope(ticketScope)
                .description(description)
                .build();
    }

    public void setTicketCode(String ticketCode) {
        if (StringUtils.isEmpty(ticketCode)) {
            throw new IllegalArgumentException("Ticket code cannot be empty");
        }
        this.ticketCode = ticketCode;
    }
}
