package com.sep490.hdbhms.billingandpayment.infrastructure.adapter;


import com.sep490.hdbhms.billingandpayment.application.port.out.ExternalPaymentPort;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent;

public class PayOSAdapter implements ExternalPaymentPort {
    @Override
    public PaymentIntent createCheckoutRequest(PaymentRequest request) {
        return null;
    }

    @Override
    public PaymentStatus checkPaymentStatus(String paymentId) {
        return null;
    }
}
