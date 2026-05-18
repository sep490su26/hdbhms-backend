package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;


import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;

public record PaymentIntent(String id, String checkOutUrl, PaymentIntentProvider paymentIntentProvider,
                            PaymentStatus paymentStatus) {

}
