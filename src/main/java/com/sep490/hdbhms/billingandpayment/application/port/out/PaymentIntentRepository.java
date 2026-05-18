package com.sep490.hdbhms.billingandpayment.application.port.out;

import com.sep490.hdbhms.billingandpayment.domain.model.PaymentIntent;

import java.util.Optional;

public interface PaymentIntentRepository {
    PaymentIntent save(PaymentIntent paymentIntent);

    Optional<PaymentIntent> findById(Long id);
}
