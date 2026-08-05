package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.VisitRequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestStatusUpdateRequest {
    @NotNull(message = "VISIT_004")
    VisitRequestStatus status;
}
