package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionProvider;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.TransactionStatus;
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
public class PaymentTransaction {
    Long id;
    TransactionProvider provider;
    String providerTransactionId;
    Long collectionAccountId;
    Long amount;
    LocalDateTime transactionTime;
    String payerName;
    String payerAccount;
    String content;
    TransactionStatus status;
    byte[] rawPayload;
    Long confirmedBy;
    LocalDateTime confirmedAt;
    LocalDateTime createdAt;

    public void setMatched(){
        this.status = TransactionStatus.MATCHED;
    }

    public void reject() {
        this.status = TransactionStatus.REJECTED;
    }
}
