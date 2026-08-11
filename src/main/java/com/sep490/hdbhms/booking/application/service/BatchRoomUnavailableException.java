package com.sep490.hdbhms.booking.application.service;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class BatchRoomUnavailableException extends AppException {
    private final List<UnavailableRoom> unavailableRooms;
    private final List<AvailableRoom> availableRooms;

    public BatchRoomUnavailableException(
            List<UnavailableRoom> unavailableRooms,
            List<AvailableRoom> availableRooms
    ) {
        super(ApiErrorCode.BATCH_ROOM_UNAVAILABLE);
        this.unavailableRooms = unavailableRooms;
        this.availableRooms = availableRooms;
    }

    @Override
    public Object getResponseData() {
        return Map.of(
                "unavailableRooms", unavailableRooms,
                "availableRooms", availableRooms
        );
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
