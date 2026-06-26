package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.util.List;

public record PublicPropertyFloorPlanResponse(
        Long propertyId,
        String propertyName,
        List<FloorPlanLayoutResponse> floors
) {
}
