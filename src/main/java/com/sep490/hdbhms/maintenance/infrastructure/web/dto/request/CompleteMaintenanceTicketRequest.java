package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteMaintenanceTicketRequest {
    @JsonAlias({"workerName", "worker_name"})
    String workerName;

    @JsonAlias({"repairItems", "repair_items"})
    String repairItems;

    @JsonAlias({"rootCause", "root_cause"})
    String rootCause;

    @JsonAlias({"ticketScope", "ticket_scope"})
    TicketScope ticketScope;

    CostType costType;

    @JsonAlias({"costDescription", "cost_description"})
    String costDescription;

    Long amount;

    PaidBy paidBy;

    @JsonAlias({"completionNote", "completion_note"})
    String completionNote;
}
