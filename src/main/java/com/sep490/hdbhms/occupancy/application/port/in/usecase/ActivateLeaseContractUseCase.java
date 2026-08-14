package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.ActivateLeaseContractRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface ActivateLeaseContractUseCase {
    LeaseContractManagementResponse execute(Long leaseContractId, ActivateLeaseContractRequest request);
}
