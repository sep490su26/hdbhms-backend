package com.sep490.hdbhms.maintenance.application.port.in.command;

import com.sep490.hdbhms.maintenance.domain.valueObjects.Priority;
import com.sep490.hdbhms.maintenance.domain.valueObjects.TicketScope;

import java.util.List;

public record CreateMaintenanceTicketCommand(
        Long roomId,
        String type,
        String category,
        String title,
        TicketScope ticketScope,
        Priority priority,
        String description,
        List<Long> attachmentIds
) {
}
