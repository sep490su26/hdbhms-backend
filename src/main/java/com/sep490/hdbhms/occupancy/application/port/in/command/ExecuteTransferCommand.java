package com.sep490.hdbhms.occupancy.application.port.in.command;

public record ExecuteTransferCommand(
        Long requestId,
        Long executedById
) {}
