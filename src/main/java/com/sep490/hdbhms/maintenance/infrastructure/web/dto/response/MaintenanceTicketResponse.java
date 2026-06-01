package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTicketResponse {
    Long id;
    String ticketCode;
    String description;
    MaintenanceTicketStatus status;
    LocalDateTime createdAt;
}
