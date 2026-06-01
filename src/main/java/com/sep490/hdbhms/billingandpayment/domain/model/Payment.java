package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentIntentProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.PaymentStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {
    final String id;

    PaymentIntentProvider provider;
    Long accountId;
    int amount;
    @Builder.Default
    PaymentStatus status = PaymentStatus.PENDING;

    @Builder.Default
    final Instant createdAt = Instant.now();
    @Builder.Default
    Instant lastUpdatedAt = Instant.now();

    public static Payment newPayment(Long accountId, int amount) {
        return Payment.builder()
                .accountId(accountId)
                .amount(amount)
                .build();
    }

    public void setProvider(PaymentIntentProvider provider) {
        this.provider = provider;
        this.lastUpdatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = PaymentStatus.SUCCEEDED;
        this.lastUpdatedAt = Instant.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
        this.lastUpdatedAt = Instant.now();
    }
}
