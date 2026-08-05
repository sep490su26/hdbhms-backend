package com.sep490.hdbhms.property.application.port.in.command;

import com.sep490.hdbhms.property.domain.value_objects.PropertyType;

public record CreatePropertyCommand(
        String name,
        PropertyType propertyType,
        String addressLine,
        String description
) {
}
