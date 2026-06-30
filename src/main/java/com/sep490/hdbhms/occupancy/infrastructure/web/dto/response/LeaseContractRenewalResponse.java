package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.LeaseStatus;
import com.sep490.hdbhms.occupancy.domain.valueObjects.OccupantRole;

import java.util.List;

public record LeaseContractRenewalResponse(
        Long oldContractId,
        String oldContractCode,
        LeaseStatus oldContractStatus,
        Long newContractId,
        String newContractCode,
        LeaseStatus newContractStatus,
        Long previousContractId,
        Long roomId,
        String roomCode,
        Integer occupantsCopiedCount,
        List<OccupantInfo> occupants
) {
    public record OccupantInfo(
            Long tenantProfileId,
            String fullName,
            String phone,
            OccupantRole occupantRole
    ) {
    }
}
