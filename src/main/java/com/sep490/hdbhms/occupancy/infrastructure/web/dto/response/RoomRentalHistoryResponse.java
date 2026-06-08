package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import java.util.List;

public record RoomRentalHistoryResponse(
        Long roomId,
        String roomCode,
        String roomName,
        List<LeaseContractQueryDetailsResponse> contracts
) {
}
