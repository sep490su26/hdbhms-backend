package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;


import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.PaymentStatus;

import java.time.LocalDateTime;

public record PaymentIntent(
        Long id,
        String checkOutUrl,
        PaymentIntentProvider paymentIntentProvider,
        PaymentStatus paymentStatus,
        Long amount,
        String providerOrderCode,
        String paymentContent,
        String qrCode,
        String qrPayload,
        LocalDateTime expiresAt,
        Long orderCode,
        String paymentLinkId,
        String bankBin,
        String bankShortName,
        String accountNumber,
        String accountName,
        String transferDescription
) {

}
