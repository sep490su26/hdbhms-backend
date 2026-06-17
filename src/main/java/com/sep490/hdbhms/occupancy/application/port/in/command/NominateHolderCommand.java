package com.sep490.hdbhms.occupancy.application.port.in.command;

public record NominateHolderCommand(
        Long requestId,
        Long requesterId,
        Long nominatedHolderProfileId
) {}
