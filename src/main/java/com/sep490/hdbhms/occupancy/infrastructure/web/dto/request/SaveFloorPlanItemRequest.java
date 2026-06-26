package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record SaveFloorPlanItemRequest(
        Long id,
        @NotBlank String itemType,
        Long roomId,
        String label,
        @NotNull BigDecimal x,
        @NotNull BigDecimal y,
        @NotNull @DecimalMin(value = "0.01") BigDecimal width,
        @NotNull @DecimalMin(value = "0.01") BigDecimal height,
        BigDecimal rotation,
        Integer sortOrder,
        Map<String, Object> metadata
) {
}
