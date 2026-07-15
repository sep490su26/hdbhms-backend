package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyStatusRequest(
        @NotNull PropertyStatus status
) {
}
