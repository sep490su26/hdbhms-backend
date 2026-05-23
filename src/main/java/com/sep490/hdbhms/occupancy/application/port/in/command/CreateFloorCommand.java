package com.sep490.hdbhms.occupancy.application.port.in.command;

public record CreateFloorCommand(
        Long propertyId,
        String floorCode,
        String name,
        Integer sortOrder
) {
}
