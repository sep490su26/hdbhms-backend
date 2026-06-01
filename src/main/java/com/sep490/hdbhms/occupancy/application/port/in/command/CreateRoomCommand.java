package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.math.BigDecimal;

public record CreateRoomCommand(
        Long propertyId,
        Long floorId,
        String roomCode,
        String name,
        BigDecimal areaM2,
        Long listedPrice,
        Integer maxOccupants,
        Integer sortOrder
) {
}
