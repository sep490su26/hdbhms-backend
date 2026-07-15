package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRuleViolationRequest {
    Long propertyId;
    Long roomId;
    Long occupantId;
    String violationType;
    Long amount;
    String description;
    Boolean includeInMonthlyInvoice;
    String collectionMethod;
    String billingPeriod;
    LocalDate occurredAt;
    List<Long> attachmentIds;
}
