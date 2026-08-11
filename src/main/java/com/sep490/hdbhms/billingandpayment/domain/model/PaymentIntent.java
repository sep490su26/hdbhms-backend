package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentStatus;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
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
    Long depositBatchId;
    Long amount;
    String providerOrderCode;
    PaymentIntentProvider provider;
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
            String paymentContent,
            LocalDateTime expiresAt
    ) {
        return PaymentIntent.builder()
                .invoiceId(invoiceId)
                .depositAgreementId(depositAgreementId)
                .amount(amount)
                .provider(provider)
                .paymentContent(paymentContent)
                .status(PaymentIntentStatus.PENDING)
                .expiresAt(expiresAt)
                .build();
    }

    public void succeedPayment() {
        if (this.status != PaymentIntentStatus.PENDING
                && this.status != PaymentIntentStatus.EXPIRED) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = PaymentIntentStatus.SUCCEEDED;
    }

    public void failPayment() {
        if (this.status != PaymentIntentStatus.PENDING) {
            throw new AppException(ApiErrorCode.INVALID_REQUEST_STATE);
        }
        this.status = PaymentIntentStatus.FAILED;
    }

    public void expirePayment() {
        if (this.status != PaymentIntentStatus.PENDING
                && this.status != PaymentIntentStatus.CREATED) {
            return;
        }
        this.status = PaymentIntentStatus.EXPIRED;
    }

    public void attachQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public void attachProviderOrderCode(String orderCode) {
        this.providerOrderCode = orderCode;
    }

    public void requireRefund() {
        this.status = PaymentIntentStatus.REFUND_REQUIRED;
    }
}
