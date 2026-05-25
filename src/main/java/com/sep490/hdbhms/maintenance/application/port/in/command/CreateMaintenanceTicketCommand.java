package com.sep490.hdbhms.maintenance.application.port.in.command;

import java.util.List;

public record CreateMaintenanceTicketCommand(String type,
                                             String description,
                                             List<Long> attachmentIds) {
}
