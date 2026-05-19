package com.sep490.hdbhms.maintenance.application.port.in.usecase;

import com.sep490.hdbhms.maintenance.application.port.in.query.GetListMaintenanceTicketsQuery;
import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import org.springframework.data.domain.Page;

public interface GetListMaintenanceTicketsUseCase {
    Page<MaintenanceTicket> execute(GetListMaintenanceTicketsQuery query);
}
