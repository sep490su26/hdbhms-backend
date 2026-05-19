package com.sep490.hdbhms.modules.rule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record PropertyRuleItemResponse(
        Long id,

        @JsonProperty("rule_code")
        String ruleCode,

        @JsonProperty("rule_category")
        String ruleCategory,

        @JsonProperty("icon_key")
        String iconKey,

        String title,
        String description,

        @JsonProperty("default_fine_amount")
        BigDecimal defaultFineAmount,

        @JsonProperty("fine_unit")
        String fineUnit,

        @JsonProperty("is_highlight")
        Boolean isHighlight,

        @JsonProperty("display_note")
        String displayNote,

        @JsonProperty("sort_order")
        Integer sortOrder,

        String status
) {
}
