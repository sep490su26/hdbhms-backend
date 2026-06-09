package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.AssetCondition;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomAssetResponse {
    Long id;
    Long roomId;
    String assetName;
    String assetCategory;
    Integer quantity;
    AssetCondition currentCondition;
    String description;
    Long fileImageId;
}
