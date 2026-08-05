package com.sep490.hdbhms.booking.application.port.in.command;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;

public record ConfirmDepositPaymentCommand(Long paymentIntentId, PaymentStatus paymentStatus) {
}
