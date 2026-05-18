package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentIntent {
    Long id;
    Long invoiceId;
    Long depositAgreementId;
    Long invoicePaymentGroupId;
    Long amount;
    PaymentIntentProvider provider;
    Long collectionAccountId;
    String paymentContent;
    String qrPayload;
    PaymentIntentStatus status;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;

    public static PaymentIntent newDepositPaymentIntent(
            Long invoiceId,
            Long depositAgreementId,
            Long amount,
            PaymentIntentProvider provider,
            String paymentContent
    ) {
        return PaymentIntent.builder()
                .invoiceId(invoiceId)
                .depositAgreementId(depositAgreementId)
                .amount(amount)
                .provider(provider)
                .paymentContent(paymentContent)
                .status(PaymentIntentStatus.PENDING)
                .build();
    }

    public void succeedPayment() {
        if (this.status != PaymentIntentStatus.PENDING) {
            throw new IllegalStateException("Payment status is not PENDING");
        }
        this.status = PaymentIntentStatus.SUCCEEDED;
    }

    public void failPayment() {
        if (this.status != PaymentIntentStatus.PENDING) {
            throw new IllegalStateException("Payment status is not PENDING");
        }
        this.status = PaymentIntentStatus.FAILED;
    }
}
