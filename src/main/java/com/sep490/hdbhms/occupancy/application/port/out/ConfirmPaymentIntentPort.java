package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.occupancy.domain.model.DepositAgreement;

public interface ConfirmPaymentIntentPort {
    DepositAgreement execute(Long paymentIndentId, PaymentStatus paymentStatus);
}
