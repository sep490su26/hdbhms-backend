package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewMaintenanceTicketRequest {
    Integer rating;
    @Size(max = 500, message = "Nhận xét tối đa 500 ký tự")
    String comment;
}
