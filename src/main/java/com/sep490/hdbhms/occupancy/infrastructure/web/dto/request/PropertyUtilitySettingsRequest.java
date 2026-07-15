package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyUtilitySettingsRequest {
    Long electricityUnitPrice;
    Long electricityFreeAllowance;
    Long waterUnitPrice;
    Long waterFreeAllowance;
}
