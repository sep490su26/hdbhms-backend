package com.sep490.hdbhms.billingandpayment.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

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
}
