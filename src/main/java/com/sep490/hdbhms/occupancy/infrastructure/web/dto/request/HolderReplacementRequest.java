package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.constraints.NotNull;

public record HolderReplacementRequest(
        @NotNull(message = "Vui lòng chọn người được đề cử")
        Long nominatedHolderProfileId
) {}
