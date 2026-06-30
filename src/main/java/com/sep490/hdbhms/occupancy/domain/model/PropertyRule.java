package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RuleStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyRule {
    Long id;
    Long propertyId;
    String ruleCode;
    String title;
    String description;
    Long defaultFineAmount;
    @Builder.Default
    Integer sortOrder = 0;
    @Builder.Default
    RuleStatus status = RuleStatus.ACTIVE;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
