package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomAssetRequest(
        @NotBlank(message = "Asset name is required")
        String assetName,
        @NotBlank(message = "Asset name is required")
        String asset_name,
        @NotBlank(message = "Asset category is required")
        String assetCategory,
        @NotBlank(message = "Asset category is required")
        String asset_category,
        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,
        @NotNull(message = "Current condition is required")
        AssetCondition currentCondition,
        @NotNull(message = "Current condition is required")
        AssetCondition current_condition,
        String description,
        Long fileImageId,
        Long file_image_id
) {
    public String resolvedAssetName() {
        return first(assetName, asset_name);
    }

    public String resolvedAssetCategory() {
        return first(assetCategory, asset_category);
    }

    public AssetCondition resolvedCurrentCondition() {
        return currentCondition != null ? currentCondition : current_condition;
    }

    public Long resolvedFileImageId() {
        return fileImageId != null ? fileImageId : file_image_id;
    }

    private String first(String camelCase, String snakeCase) {
        if (camelCase != null && !camelCase.isBlank()) {
            return camelCase.trim();
        }
        return snakeCase != null ? snakeCase.trim() : null;
    }
}
