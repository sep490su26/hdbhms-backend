package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DepositCheckoutResponse(
        Long id,
        Long paymentIntentId,
        Long invoiceId,
        Long depositAgreementId,
        Long amount,
        String paymentContent,
        String description,
        String checkoutUrl,
        String checkOutUrl,
        String qrCode,
        String qrPayload,
        LocalDateTime expiresAt,
        PaymentIntentProvider provider,
        PaymentIntentStatus status,
        String orderCode,
        String providerOrderCode,
        String paymentLinkId,
        String bankBin,
        String bankShortName,
        String accountName,
        String transferDescription,
        String receiverName,
        String bankName,
        String accountNumber
) {
}
