package com.sep490.hdbhms.occupancy.infrastructure.web.controller;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyRulesQuery;
import com.sep490.hdbhms.occupancy.application.port.in.usecase.GetPropertyRulesUseCase;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertyRuleResponse;
import com.sep490.hdbhms.occupancy.infrastructure.web.mapper.PropertyRuleWebMapper;
import com.sep490.hdbhms.shared.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/properties/{propertyId}/rules")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PropertyRuleController {
    GetPropertyRulesUseCase getPropertyRulesUseCase;
    PropertyRuleWebMapper propertyRuleWebMapper;

    @GetMapping
    public ApiResponse<List<PropertyRuleResponse>> getPropertyRules(@PathVariable Long propertyId) {
        return ApiResponse.<List<PropertyRuleResponse>>builder()
                .data(getPropertyRulesUseCase.execute(new GetPropertyRulesQuery(propertyId))
                        .stream()
                        .map(propertyRuleWebMapper::toResponse)
                        .toList())
                .build();
    }
}
