package com.sep490.hdbhms.modules.rule.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PropertyRuleResponse(
        LocalDateTime updatedAt,

        List<PropertyRuleItemResponse> items
) {
}
