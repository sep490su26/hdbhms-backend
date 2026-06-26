package com.sep490.hdbhms.modules.rule.dto;

import java.math.BigDecimal;

public record PropertyRuleItemResponse(
        Long id,
        String ruleCode,
        String ruleCategory,
        String iconKey,

        String title,
        String description,
        BigDecimal defaultFineAmount,
        String fineUnit,
        Boolean isHighlight,
        String displayNote,
        Integer sortOrder,

        String status
) {
}
