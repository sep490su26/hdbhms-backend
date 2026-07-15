package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyUtilitySettingsResponse {
    Long propertyId;
    String propertyName;
    UtilitySetting electricity;
    UtilitySetting water;

    @Data
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UtilitySetting {
        Long unitPrice;
        Long freeAllowance;
        LocalDate effectiveFrom;
        LocalDate effectiveTo;
    }
}
