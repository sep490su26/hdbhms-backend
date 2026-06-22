package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.SubmitTransferInspectionCommand;

public interface ContractInspectionUseCase {
    void submitTransferInspection(SubmitTransferInspectionCommand command);
}
