package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyType;

public record CreatePropertyCommand(
        String name,
        PropertyType propertyType,
        String addressLine,
        String description
) {
}
