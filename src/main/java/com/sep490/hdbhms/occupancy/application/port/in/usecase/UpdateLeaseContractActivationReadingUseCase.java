package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateLeaseContractActivationReadingRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface UpdateLeaseContractActivationReadingUseCase {
    LeaseContractManagementResponse execute(
            Long leaseContractId,
            UpdateLeaseContractActivationReadingRequest request
    );
}
