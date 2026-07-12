package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TenantInvoiceResponse {
    Long id;
    String invoiceCode;
    String invoiceType;
    String billingPeriod;
    String status;
    Long propertyId;
    String propertyName;
    Long roomId;
    String roomCode;
    Long contractId;
    String contractCode;
    LocalDateTime issueDate;
    LocalDateTime dueDate;
    LocalDateTime issuedAt;
    LocalDateTime paidAt;
    Long subtotalAmount;
    Long discountAmount;
    Long totalAmount;
    Long paidAmount;
    Long remainingAmount;
    Long paymentIntentId;
    String checkoutUrl;
    String qrCode;
    String providerOrderCode;
    String paymentLinkId;
    String bankBin;
    String bankShortName;
    String accountNumber;
    String accountName;
    String transferDescription;
    Boolean hasOpenMeterReadingReview;
    List<TenantInvoiceLineResponse> lines;
}
