package com.sep490.hdbhms.maintenance.infrastructure.web.mapper;

import com.sep490.hdbhms.maintenance.domain.model.MaintenanceTicket;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketDetailsResponse;
import com.sep490.hdbhms.maintenance.infrastructure.web.dto.response.MaintenanceTicketResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public abstract class MaintenanceTicketWebMapper {
    public abstract MaintenanceTicketDetailsResponse toDetailsResponse(MaintenanceTicket maintenanceTicket);

    public abstract MaintenanceTicketResponse toResponse(MaintenanceTicket maintenanceTicket);
}
