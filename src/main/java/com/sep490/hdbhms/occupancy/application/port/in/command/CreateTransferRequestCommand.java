package com.sep490.hdbhms.occupancy.application.port.in.command;

import java.time.LocalDate;
import java.util.List;

public record CreateTransferRequestCommand(
        Long requesterId,
        Long sourceContractId,
        Long targetRoomId,
        LocalDate requestedTransferDate,
        String reason
) {
}
