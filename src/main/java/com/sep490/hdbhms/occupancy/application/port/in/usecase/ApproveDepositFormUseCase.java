package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveDepositFormCommand;

public interface ApproveDepositFormUseCase {
    void approveAndInitiatePayment(ApproveDepositFormCommand command);

}
