package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SaveFloorPlanItemRequest(
        Long id,
        @NotBlank(message = "Vui lòng chọn loại phần tử sơ đồ tầng") @JsonAlias("itemType") String type,
        Long roomId,
        @NotNull @JsonAlias("x") Integer positionX,
        @NotNull @JsonAlias("y") Integer positionY,
        @NotNull(message = "Chiều rộng là bắt buộc") @Min(value = 1, message = "Chiều rộng phải lớn hơn hoặc bằng 1") Integer width,
        @NotNull(message = "Chiều cao là bắt buộc") @Min(value = 1, message = "Chiều cao phải lớn hơn hoặc bằng 1") Integer height,
        Map<String, Object> metadata
) {
}
