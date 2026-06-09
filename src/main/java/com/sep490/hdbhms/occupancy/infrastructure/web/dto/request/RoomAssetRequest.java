package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;

public record RoomAssetRequest(
        String assetName,
        String asset_name,
        String assetCategory,
        String asset_category,
        Integer quantity,
        AssetCondition currentCondition,
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
