package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record HolderReplacementRequest(
        @NotNull(message = "Nominated holder profile ID is required")
        Long nominatedHolderProfileId
) {}
