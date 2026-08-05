package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface CreateDraftLeaseContractForDepositUseCase {
    LeaseContractManagementResponse execute(Long depositAgreementId);
}
