package com.sep490.hdbhms.maintenance.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RuleViolationResponse {
    Long id;
    Long ticketId;
    String ticketCode;
    String violationType;
    String lineType;
    Long amount;
    String status;
    String billingStatus;
    String billingStatusLabel;
    Long invoiceId;
    String invoiceCode;
    String invoiceStatus;
    Long invoiceLineId;
    String checkoutUrl;
    String providerOrderCode;
    String message;
}
