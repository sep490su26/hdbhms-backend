package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdatePropertyRequest(
        @NotBlank String name,
        @NotNull PropertyType propertyType,
        @NotBlank String addressLine,
        String description,
        @NotNull PropertyStatus status
) {
}
