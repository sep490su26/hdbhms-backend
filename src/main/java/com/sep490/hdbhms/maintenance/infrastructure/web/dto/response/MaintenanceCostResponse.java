package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import com.sep490.hdbhms.maintenance.domain.valueObjects.CostType;
import com.sep490.hdbhms.maintenance.domain.valueObjects.PaidBy;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MaintenanceCostResponse {
    Long id;
    CostType costType;
    String description;
    Long amount;
    PaidBy paidBy;
    LocalDateTime createdAt;
}
