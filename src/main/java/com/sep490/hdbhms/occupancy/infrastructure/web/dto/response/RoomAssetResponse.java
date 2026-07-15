package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;

public record RoomAssetResponse(
        Long id,
        Long roomId,
        String assetName,
        String assetCategory,
        Integer quantity,
        AssetCondition currentCondition,
        String description,
        Long fileImageId
) {
}
