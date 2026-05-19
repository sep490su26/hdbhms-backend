package com.sep490.hdbhms.occupancy.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;
import com.sep490.hdbhms.occupancy.domain.model.DepositForm;

public interface SendDepositPaymentPort {
    PaymentIntent execute(DepositForm depositForm);
}
