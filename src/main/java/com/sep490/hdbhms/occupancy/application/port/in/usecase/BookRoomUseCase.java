package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.occupancy.application.port.in.command.SendDepositFormCommand;

public interface BookRoomUseCase {
    PaymentIntent initDepositForm(SendDepositFormCommand command);
}
