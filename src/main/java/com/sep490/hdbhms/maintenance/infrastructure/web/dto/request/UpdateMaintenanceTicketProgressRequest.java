package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMaintenanceTicketProgressRequest {
    @JsonAlias({"workerName", "worker_name"})
    String workerName;

    @JsonAlias({"repairmanName", "repairman_name"})
    String repairmanName;

    @JsonAlias({"repairmanPhone", "repairman_phone"})
    String repairmanPhone;

    @JsonAlias({"repairItems", "repair_items"})
    String repairItems;

    @JsonAlias({"rootCause", "root_cause"})
    String rootCause;

    @JsonAlias({"ticketScope", "ticket_scope"})
    TicketScope ticketScope;

    String note;
}
