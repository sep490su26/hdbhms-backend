package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.UtilityType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UtilityTariff {
    Long id;
    Long propertyId;
    UtilityType utilityType;
    Long unitPrice;
    @Builder.Default
    Long freeAllowance = 0L;
    Long serviceFeeWaiveElectricityThreshold;
    LocalDate effectiveFrom;
    LocalDate effectiveTo;
    Long createdById;
    LocalDateTime createdAt;
}
