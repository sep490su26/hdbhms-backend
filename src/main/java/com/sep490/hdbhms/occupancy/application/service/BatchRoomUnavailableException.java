package com.sep490.hdbhms.occupancy.application.service;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;

import java.util.List;

@Getter
public class BatchRoomUnavailableException extends RuntimeException {
    private final List<UnavailableRoom> unavailableRooms;
    private final List<AvailableRoom> availableRooms;

    public BatchRoomUnavailableException(
            List<UnavailableRoom> unavailableRooms,
            List<AvailableRoom> availableRooms
    ) {
        super("Một số phòng đã có người đặt cọc hoặc đang được xử lý.");
        this.unavailableRooms = unavailableRooms;
        this.availableRooms = availableRooms;
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record UnavailableRoom(
            Long roomId,
            String roomCode,
            String reason,
            String message
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record AvailableRoom(Long roomId, String roomCode) {
    }
}
