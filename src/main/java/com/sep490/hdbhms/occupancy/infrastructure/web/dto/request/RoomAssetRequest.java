package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RoomAssetRequest(
        @NotBlank(message = "Asset name is required")
        String assetName,
        @NotBlank(message = "Asset category is required")
        String assetCategory,
        @NotNull(message = "Quantity is required")
        @PositiveOrZero(message = "Quantity must not be negative")
        Integer quantity,
        @NotNull(message = "Current condition is required")
        AssetCondition currentCondition,
        String description,
        Long fileImageId
) {
}
