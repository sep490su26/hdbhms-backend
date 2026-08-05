package com.sep490.hdbhms.property.infrastructure.web.dto.request;

import com.sep490.hdbhms.property.domain.value_objects.RuleStatus;

public record UpsertPropertyRuleRequest(
        String ruleCode,
        String title,
        String description,
        Long defaultFineAmount,
        Integer sortOrder,
        RuleStatus status
) {
}
