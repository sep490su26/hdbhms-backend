package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.LeaseContractLiquidationCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface UpdateLeaseLiquidationDraftUseCase {
    LeaseContractManagementResponse execute(LeaseContractLiquidationCommand command);
}
