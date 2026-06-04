package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;


import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentIntent(
        Long id,
        String checkOutUrl,
        PaymentIntentProvider paymentIntentProvider,
        PaymentStatus paymentStatus,
        Long amount,
        String paymentContent,
        String qrCode,
        String qrPayload,
        LocalDateTime expiresAt,
        Long orderCode,
        String paymentLinkId
) {

}
