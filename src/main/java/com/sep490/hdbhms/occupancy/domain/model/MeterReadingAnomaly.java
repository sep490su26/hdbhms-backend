package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.AnomalySeverity;
import com.sep490.hdbhms.occupancy.domain.value_objects.AnomalyType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MeterReadingAnomaly {
    Long id;
    Long meterReadingId;
    AnomalyType anomalyType;
    String message;
    @Builder.Default
    AnomalySeverity severity = AnomalySeverity.MEDIUM;
    LocalDateTime resolvedAt;
    Long resolvedById;
    LocalDateTime createdAt;
}
