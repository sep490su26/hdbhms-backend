package com.sep490.hdbhms.billingandpayment.application.port.in.usecase;

import com.sep490.hdbhms.billingandpayment.application.port.in.command.ReconcilePaymentCommand;

public interface ReconcilePaymentUseCase {
    void execute(ReconcilePaymentCommand command);
}
