package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewMaintenanceTicketRequest {
    Integer rating;
    String comment;
}
