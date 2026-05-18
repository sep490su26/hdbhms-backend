package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.command.ApproveDepositFormCommand;
import com.sep490.hdbhms.occupancy.application.port.in.command.ConfirmDepositPaymentCommand;

public interface ApproveDepositFormUseCase {
    void approveAndInitiatePayment(ApproveDepositFormCommand command);

    void confirmPayment(ConfirmDepositPaymentCommand command);
}
