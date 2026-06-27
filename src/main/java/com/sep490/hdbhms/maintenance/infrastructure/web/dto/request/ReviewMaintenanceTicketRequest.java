package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewMaintenanceTicketRequest {
    Integer rating;
    @JsonAlias({"feedback"})
    String comment;
}
