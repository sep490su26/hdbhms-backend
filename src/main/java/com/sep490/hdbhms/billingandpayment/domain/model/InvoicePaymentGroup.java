package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoicePaymentGroupStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoicePaymentGroupType;
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
public class InvoicePaymentGroup {
    Long id;
    Long invoiceId;
    Long collectionAccountId;
    InvoicePaymentGroupType groupType;
    Long amount;
    Long paymentIntentId;
    InvoicePaymentGroupStatus status;
    LocalDateTime createdAt;
}
