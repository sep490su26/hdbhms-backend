package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VisitRequestStatsResponse {
    long todayCount;
    long pendingCount;
    long viewedCount;
    int closingRate;
}
