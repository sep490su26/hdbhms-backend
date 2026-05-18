package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.value_objects.InvoiceType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Invoice {
    final Long id;
    String invoiceCode;
    Long propertyId;
    Long roomId;
    Long contractId;
    InvoiceType invoiceType;
    @Builder.Default
    Integer revisionNo = 1;
    String billingPeriod;
    LocalDate issueDate;
    LocalDate dueDate;
    InvoiceStatus status;

    @Builder.Default
    Long subtotalAmount = 0L;
    @Builder.Default
    Long discountAmount = 0L;
    @Builder.Default
    Long totalAmount = 0L;
    @Builder.Default
    Long paidAmount = 0L;
    @Builder.Default
    Long remainingAmount = 0L;

    Long collectionAccountId;
    Long createdBy;
    LocalDateTime issuedAt;
    LocalDateTime voidedAt;
    String voidReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @Builder.Default
    Integer version = 0;
    String activeInvoiceKey;

    public static Invoice newDepositInvoice(String invoiceCode, Long propertyId, Long roomId) {
        return Invoice.builder()
                .invoiceCode(invoiceCode)
                .propertyId(propertyId)
                .dueDate(LocalDate.now().plusDays(1))
                .roomId(roomId)
                .invoiceType(InvoiceType.DEPOSIT)
                .issueDate(LocalDate.now())
                .issuedAt(LocalDateTime.now())
                .status(InvoiceStatus.ISSUED)
                .build();
    }
}
