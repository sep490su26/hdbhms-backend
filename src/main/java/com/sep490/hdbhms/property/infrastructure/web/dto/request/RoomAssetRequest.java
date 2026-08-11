package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.AssetCondition;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record RoomAssetRequest(
        @NotBlank(message = "Vui lòng nhập tên tài sản")
        String assetName,
        @NotBlank(message = "Vui lòng nhập loại tài sản")
        String assetCategory,
        @NotNull(message = "Vui lòng nhập số lượng")
        @PositiveOrZero(message = "Số lượng không được nhỏ hơn 0")
        Integer quantity,
        @NotNull(message = "Vui lòng chọn tình trạng tài sản")
        AssetCondition currentCondition,
        String description,
        Long fileImageId
) {
}
