package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;

public interface ConfirmPaymentIntentPort {
    void execute(Long paymentIndentId, PaymentStatus paymentStatus);
}
