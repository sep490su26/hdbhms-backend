package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import java.util.List;

public record FloorPlanLayoutResponse(
        Long propertyId,
        String propertyName,
        Long floorId,
        String floorName,
        Integer floorNumber,
        Integer savedCount,
        List<FloorPlanItemResponse> items
) {
}
