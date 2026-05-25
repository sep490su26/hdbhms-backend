package com.sep490.hdbhms.modules.rule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record PropertyRuleResponse(
        @JsonProperty("updated_at")
        LocalDateTime updatedAt,

        List<PropertyRuleItemResponse> items
) {
}
