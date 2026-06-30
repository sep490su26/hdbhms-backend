package com.sep490.hdbhms.occupancy.application.port.in.command;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentStatus;

public record ConfirmDepositPaymentCommand(Long paymentIntentId, PaymentStatus paymentStatus) {
}
