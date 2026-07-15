package com.sep490.hdbhms.occupancy.application.port.in.command;

public record AcceptHolderNominationCommand(
        Long requestId,
        Long tenantId
) {}
