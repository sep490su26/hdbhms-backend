package com.sep490.hdbhms.billingandpayment.application.port.in.usecase;

import com.sep490.hdbhms.billingandpayment.application.port.in.query.GetPaymentIntentQuery;
import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;

public interface GetPaymentIntentUseCase {
    PaymentIntent execute(GetPaymentIntentQuery query);
}
