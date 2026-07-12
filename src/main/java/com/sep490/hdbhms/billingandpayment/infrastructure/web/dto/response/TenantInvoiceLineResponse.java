package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TenantInvoiceLineResponse {
    Long id;
    String lineType;
    String description;
    Integer quantity;
    Long unitPrice;
    Long amount;
    String sourceType;
    Long sourceId;
    Long meterReadingId;
    String meterType;
    String readingPeriod;
    BigDecimal previousValue;
    BigDecimal currentValue;
    BigDecimal usageAmount;
    LocalDate readingDate;
    Long photoFileId;
    String reviewStatus;
    Integer reviewCount;
    Long openReviewId;
    Boolean canComplain;
}
