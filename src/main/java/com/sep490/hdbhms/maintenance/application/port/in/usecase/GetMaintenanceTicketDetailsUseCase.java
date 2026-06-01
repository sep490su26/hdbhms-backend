package com.sep490.hdbhms.maintenance.application.port.in.usecase;

import com.sep490.hdbhms.maintenance.application.port.in.query.GetMaintenanceTicketDetailsQuery;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;

public interface GetMaintenanceTicketDetailsUseCase {
    MaintenanceTicket execute(GetMaintenanceTicketDetailsQuery query);
}
