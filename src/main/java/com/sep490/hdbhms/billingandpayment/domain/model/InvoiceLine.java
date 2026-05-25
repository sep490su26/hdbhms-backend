package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceLineType;
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
public class InvoiceLine {
    Long id;
    Long invoiceId;
    InvoiceLineType lineType;
    String description;
    Integer quantity;
    Long unitPrice;
    Long amount;
    Long meterReadingId;
    String sourceType;
    Long sourceId;
    Long collectionAccountId;
    LocalDateTime createdAt;

    public static InvoiceLine newDepositInvoiceLine(Long invoiceId, Long depositAmount) {
        return InvoiceLine.builder()
                .invoiceId(invoiceId)
                .lineType(InvoiceLineType.DEPOSIT_DEDUCTION)
                .quantity(1)
                .unitPrice(depositAmount)
                .description("Thanh toán")
                .build();
    }
}
