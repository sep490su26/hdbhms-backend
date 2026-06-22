package com.sep490.hdbhms.maintenance.infrastructure.web.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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
    @JsonAlias({"propertyId", "property_id"})
    Long propertyId;
    @JsonAlias({"roomId", "room_id"})
    Long roomId;
    @JsonAlias({"occupantId", "occupant_id", "tenantProfileId", "tenant_profile_id"})
    Long occupantId;
    @JsonAlias({"violationType", "violation_type"})
    String violationType;
    Long amount;
    String description;
    @JsonAlias({"includeInMonthlyInvoice", "include_in_monthly_invoice"})
    Boolean includeInMonthlyInvoice;
    @JsonAlias({"collectionMethod", "collection_method", "billingMode", "billing_mode"})
    String collectionMethod;
    @JsonAlias({"billingPeriod", "billing_period", "billingMonth", "billing_month"})
    String billingPeriod;
    @JsonAlias({"occurredAt", "occurred_at", "violationDate", "violation_date"})
    LocalDate occurredAt;
    @JsonAlias({"attachmentIds", "attachment_ids", "evidenceFileIds", "evidence_file_ids"})
    List<Long> attachmentIds;
}
