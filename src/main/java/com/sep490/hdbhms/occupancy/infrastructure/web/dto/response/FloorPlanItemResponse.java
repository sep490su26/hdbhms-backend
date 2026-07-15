package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;

import java.math.BigDecimal;
import java.util.Map;

public record FloorPlanItemResponse(
        Long id,
        String type,
        Long roomId,
        String roomCode,
        Integer positionX,
        Integer positionY,
        Integer width,
        Integer height,
        Map<String, Object> metadata,
        RoomStatus publicStatus,
        Long monthlyRent,
        BigDecimal area,
        Integer maxOccupants
) {
}
