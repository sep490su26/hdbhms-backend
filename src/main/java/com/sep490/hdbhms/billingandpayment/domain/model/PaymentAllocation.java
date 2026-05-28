package com.sep490.hdbhms.billingandpayment.domain.model;

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
public class PaymentAllocation {
    Long id;
    Long paymentTransactionId;
    Long invoiceId;
    Long amount;
    Long allocatedBy;
    LocalDateTime allocatedAt;

    public static PaymentAllocation allocate(
            Long paymentTransactionId,
            Long invoiceId,
            Long amount
    ) {
        return PaymentAllocation.builder()
                .paymentTransactionId(paymentTransactionId)
                .invoiceId(invoiceId)
                .amount(amount)
                .allocatedBy(null)
                .build();
    }

    public static PaymentAllocation manualAllocate(
            Long paymentTransactionId,
            Long invoiceId,
            Long amount,
            Long staffUserId
    ) {
        return PaymentAllocation.builder()
                .paymentTransactionId(paymentTransactionId)
                .invoiceId(invoiceId)
                .amount(amount)
                .allocatedBy(staffUserId)
                .build();
    }
}