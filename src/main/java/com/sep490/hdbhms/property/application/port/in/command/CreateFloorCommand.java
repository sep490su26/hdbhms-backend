package com.sep490.hdbhms.property.application.port.in.command;

public record CreateFloorCommand(
        Long propertyId,
        String floorCode,
        String name,
        Integer sortOrder
) {
}
