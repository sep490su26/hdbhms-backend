package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SaveFloorPlanLayoutRequest(
        @NotNull List<@Valid SaveFloorPlanItemRequest> items
) {
}
