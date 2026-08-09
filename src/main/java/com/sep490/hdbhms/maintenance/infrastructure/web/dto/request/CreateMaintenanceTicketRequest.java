package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMaintenanceTicketRequest {
    Long roomId;
    Long propertyId;
    String type;
    String category;
    String title;
    TicketScope ticketScope;
    TicketScope scope;
    String description;
    Boolean repairRequested;
    List<Long> attachmentIds;
    Long actualCost;
    String accountingNote;
    Long receiptFileId;
    CostType costType;
}
