package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import java.util.List;

public record PublicPropertyFloorPlanResponse(
        Long propertyId,
        String propertyName,
        List<FloorPlanLayoutResponse> floors
) {
}
