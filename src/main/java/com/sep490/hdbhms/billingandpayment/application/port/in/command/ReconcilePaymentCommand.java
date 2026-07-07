package com.sep490.hdbhms.billingandpayment.application.port.in.command;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.TransactionProvider;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReconcilePaymentCommand {
    Long paymentIntentId;
    TransactionProvider provider;
    String providerTransactionId;
    Long amount;
    String content;
    String payerName;
    String payerAccount;
    LocalDateTime transactionTime;
    String rawPayload;
}
