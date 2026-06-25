package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record UpdateRoomLayoutsRequest(
        @NotEmpty List<@Valid RoomLayout> rooms
) {
    public record RoomLayout(
            @NotNull Long roomId,
            @NotNull Integer positionX,
            @NotNull Integer positionY,
            String type,
            Integer width,
            Integer height,
            String orientation,
            BigDecimal areaSqm,
            List<OpeningLayout> doors,
            List<OpeningLayout> windows
    ) {
    }

    public record OpeningLayout(
            String id,
            String wall,
            BigDecimal offset
    ) {
    }
}
