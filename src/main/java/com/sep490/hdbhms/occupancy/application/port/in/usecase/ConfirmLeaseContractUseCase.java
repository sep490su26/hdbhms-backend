package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmLeaseContractCommand;

public interface ConfirmLeaseContractUseCase {
    void execute(ConfirmLeaseContractCommand command);
}
