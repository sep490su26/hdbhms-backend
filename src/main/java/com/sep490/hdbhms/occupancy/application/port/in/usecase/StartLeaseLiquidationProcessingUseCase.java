package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.StartLeaseLiquidationProcessingCommand;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.LeaseContractManagementResponse;

public interface StartLeaseLiquidationProcessingUseCase {
    LeaseContractManagementResponse execute(StartLeaseLiquidationProcessingCommand command);
}
