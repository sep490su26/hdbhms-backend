package com.sep490.hdbhms.billingandpayment.application.port.out;


import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.request.PaymentRequest;
import com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response.PaymentIntent;

public interface ExternalPaymentPort {
    PaymentIntent createCheckoutRequest(PaymentRequest request);

    PaymentStatus checkPaymentStatus(String paymentId);
}
