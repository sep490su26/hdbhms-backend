package com.sep490.hdbhms.occupancy.application.port.in.command;

public record ApproveTransferCommand(
        Long requestId,
        Long managerId
) {}
