package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

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
    String workerName;

    String repairmanName;

    String repairmanPhone;

    String repairItems;

    String rootCause;

    TicketScope ticketScope;

    CostType costType;

    String costDescription;

    Long amount;

    Long actualCost;

    PaidBy paidBy;

    CostResponsibility costResponsibility;

    Boolean chargeToTenant;

    String lineType;

    String collectionMethod;

    String billingPeriod;

    List<Long> attachmentIds;

    AttachmentPhase attachmentPhase;

    String completionNote;
}
