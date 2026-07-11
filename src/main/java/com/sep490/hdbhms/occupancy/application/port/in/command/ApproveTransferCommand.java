package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.occupancy.domain.value_objects.SettlementType;

public record ApproveTransferCommand(
        Long requestId,
        Long managerId,
        SettlementType positiveDifferenceSettlementType
) {}