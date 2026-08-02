package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import com.sep490.hdbhms.occupancy.domain.value_objects.RuleStatus;

public record UpsertPropertyRuleRequest(
        String ruleCode,
        String title,
        String description,
        Long defaultFineAmount,
        Integer sortOrder,
        RuleStatus status
) {
}
