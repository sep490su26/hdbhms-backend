package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sep490.hdbhms.maintenance.domain.value_objects.CostType;
import com.sep490.hdbhms.maintenance.domain.value_objects.Priority;
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
    @JsonAlias({"roomId", "room_id"})
    Long roomId;
    @JsonAlias({"propertyId", "property_id"})
    Long propertyId;
    String type;
    String category;
    String title;
    @JsonAlias({"ticketScope", "ticket_scope"})
    TicketScope ticketScope;
    @JsonAlias({"scope"})
    TicketScope scope;
    Priority priority;
    @JsonAlias({"severity"})
    Priority severity;
    String description;
    @JsonAlias({"attachmentIds", "attachment_ids"})
    List<Long> attachmentIds;
    @JsonAlias({"actualCost", "actual_cost"})
    Long actualCost;
    @JsonAlias({"accountingNote", "accounting_note", "costDescription", "cost_description"})
    String accountingNote;
    @JsonAlias({"receiptFileId", "receipt_file_id"})
    Long receiptFileId;
    CostType costType;
}
