package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.ViolationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RuleViolation {
    Long id;
    Long propertyId;
    Long roomId;
    Long contractId;
    Long tenantProfileId;
    Long ruleId;
    LocalDate violationDate;
    String description;
    @Builder.Default
    Long fineAmount = 0L;
    Long invoiceId;
    Long evidenceFileId;
    @Builder.Default
    ViolationStatus status = ViolationStatus.RECORDED;
    Long createdById;
    LocalDateTime createdAt;
}
