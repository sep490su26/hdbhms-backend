package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveFloorPlanLayoutRequest(
        @NotEmpty(message = "Vui lòng nhập danh sách phần tử sơ đồ tầng") List<@Valid SaveFloorPlanItemRequest> items
) {
}
