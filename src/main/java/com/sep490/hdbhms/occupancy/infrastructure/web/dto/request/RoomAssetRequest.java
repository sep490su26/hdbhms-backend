package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomAssetRequest(
        @NotBlank(message = "Asset name is required")
        String assetName,
        @NotBlank(message = "Asset category is required")
        String assetCategory,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        @NotNull(message = "Current condition is required")
        AssetCondition currentCondition,
        String description,
        Long fileImageId
) {
}