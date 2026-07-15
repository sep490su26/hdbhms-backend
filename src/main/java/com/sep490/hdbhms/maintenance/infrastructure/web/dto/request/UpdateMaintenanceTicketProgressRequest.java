package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.sep490.hdbhms.maintenance.domain.value_objects.TicketScope;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateMaintenanceTicketProgressRequest {
    String workerName;

    String repairmanName;

    String repairmanPhone;

    String repairItems;

    String rootCause;

    TicketScope ticketScope;

    String note;
}
