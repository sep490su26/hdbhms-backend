package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sep490.hdbhms.maintenance.domain.value_objects.AttachmentPhase;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostResponsibility;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import com.sep490.hdbhms.maintenance.domain.value_objects.PaidBy;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CompleteMaintenanceTicketRequest {
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

    CostType costType;

    @JsonAlias({"costDescription", "cost_description"})
    String costDescription;

    Long amount;

    @JsonAlias({"actualCost", "actual_cost"})
    Long actualCost;

    @JsonAlias({"paidBy", "paid_by", "payer"})
    PaidBy paidBy;

    @JsonAlias({"costResponsibility", "cost_responsibility"})
    CostResponsibility costResponsibility;

    @JsonAlias({"chargeToTenant", "charge_to_tenant"})
    Boolean chargeToTenant;

    @JsonAlias({"lineType", "line_type"})
    String lineType;

    @JsonAlias({"attachmentIds", "attachment_ids"})
    List<Long> attachmentIds;

    @JsonAlias({"attachmentPhase", "attachment_phase"})
    AttachmentPhase attachmentPhase;

    @JsonAlias({"completionNote", "completion_note", "resolutionNote", "resolution_note"})
    String completionNote;
}
