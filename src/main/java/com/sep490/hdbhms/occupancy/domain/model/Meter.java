package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.MeterStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.MeterType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Meter {
    Long id;
    Long roomId;
    MeterType meterType;
    String meterCode;
    @Builder.Default
    MeterStatus status = MeterStatus.ACTIVE;
    LocalDate installedAt;
    LocalDateTime createdAt;
    String activeMeterKey;
}
