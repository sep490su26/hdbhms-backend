package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateMaintenanceTicketRequest {
    String type;
    String description;
    List<Long> attachmentIds;
}
