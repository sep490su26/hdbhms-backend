package com.sep490.hdbhms.billingandpayment.domain.model;

import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceStatus;
import com.sep490.hdbhms.billingandpayment.domain.valueObjects.InvoiceType;
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
public class Invoice {
    Long id;
    String invoiceCode;
    Long propertyId;
    Long roomId;
    Long leaseContractId;
    Long depositAgreementId;
    Long depositBatchId;
    InvoiceType invoiceType;
    @Builder.Default
    Integer revisionNo = 1;
    String billingPeriod;
    LocalDateTime issueDate;
    LocalDateTime dueDate;
    InvoiceStatus status;
    Long subtotalAmount;
    @Builder.Default
    Long discountAmount = 0L;
    Long totalAmount;
    @Builder.Default
    Long paidAmount = 0L;
    Long remainingAmount;
    Long collectionAccountId;
    Long createdBy;
    LocalDateTime issuedAt;
    LocalDateTime voidedAt;
    String voidReason;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Integer version;
    String activeInvoiceKey;

    public void addDiscountAmount(long discountAmount) {
        long newDiscount = (this.getDiscountAmount() == null ? 0L : this.getDiscountAmount()) + discountAmount;
        long newTotalAmount = Math.max((this.getSubtotalAmount() == null ? 0L : this.getSubtotalAmount()) - newDiscount, 0L);
        this.discountAmount = newDiscount;
        this.totalAmount = newTotalAmount;
        this.remainingAmount = Math.max(newTotalAmount - (this.getPaidAmount() == null ? 0L : this.getPaidAmount()), 0L);
    }

    public void addSurchargeAmount(long surchargeAmount) {
        long newSubtotal = (this.getSubtotalAmount() == null ? 0L : this.getSubtotalAmount()) + surchargeAmount;
        long newTotalAmount = Math.max(newSubtotal - (this.getDiscountAmount() == null ? 0L : this.getDiscountAmount()), 0L);
        this.subtotalAmount = newSubtotal;
        this.totalAmount = newTotalAmount;
        this.remainingAmount = Math.max(newTotalAmount - (this.getPaidAmount() == null ? 0L : this.getPaidAmount()), 0L);
    }

    public void applyAmount(long amountInDong) {
        if (status == InvoiceStatus.VOIDED || status == InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Invoice cannot be paid in state " + status);
        }
        this.paidAmount += amountInDong;
        this.remainingAmount = this.totalAmount - this.paidAmount;

        if (this.remainingAmount <= 0) {
            this.status = InvoiceStatus.PAID;
        } else if (this.paidAmount > 0) {
            this.status = InvoiceStatus.PARTIALLY_PAID;
        }
    }

    public void issue() {
        if (status != InvoiceStatus.DRAFT) throw new IllegalStateException();
        this.status = InvoiceStatus.ISSUED;
        this.issuedAt = LocalDateTime.now();
    }

    public void voidInvoice(String reason) {
        if (status == InvoiceStatus.VOIDED) throw new IllegalStateException();
        this.status = InvoiceStatus.VOIDED;
        this.voidedAt = LocalDateTime.now();
        this.voidReason = reason;
    }

    public static Invoice createDepositInvoice(
            String invoiceCode,
            Long propertyId,
            Long roomId,
            Long depositAgreementId,
            Long amount,
            LocalDateTime issueDate,
            LocalDateTime dueDate,
            Long createdBy
    ) {
        return Invoice.builder()
                .invoiceCode(invoiceCode)
                .propertyId(propertyId)
                .roomId(roomId)
                .depositAgreementId(depositAgreementId)
                .invoiceType(InvoiceType.DEPOSIT)
                .issueDate(issueDate)
                .dueDate(dueDate)
                .totalAmount(amount)
                .subtotalAmount(amount)
                .remainingAmount(amount)
                .status(InvoiceStatus.ISSUED)
                .createdBy(createdBy)
                .build();
    }
}
