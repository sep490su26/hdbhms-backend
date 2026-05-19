package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.sep490.hdbhms.maintenance.domain.value_objects.MaintenanceTicketStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceTicketDetailsResponse {
    Long id;
    String ticketCode;
    String description;
    MaintenanceTicketStatus status;
    String rejectionReason;
    LocalDateTime completedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
