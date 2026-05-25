package com.sep490.hdbhms.maintenance.application.port.in.usecase;

import com.sep490.hdbhms.maintenance.application.port.in.command.CreateMaintenanceTicketCommand;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;

public interface CreateMaintenanceTicketUseCase {
    MaintenanceTicket execute(CreateMaintenanceTicketCommand command);
}
