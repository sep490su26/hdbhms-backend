package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.UpdateLeaseContractTermsCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface UpdateLeaseContractTermsUseCase {
    LeaseContractManagementResponse execute(UpdateLeaseContractTermsCommand command);
}
