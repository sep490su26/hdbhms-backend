package com.sep490.hdbhms.property.infrastructure.web.mapper;

import com.sep490.hdbhms.property.domain.model.PropertyRule;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.PropertyRuleResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PropertyRuleWebMapper {
    PropertyRuleResponse toResponse(PropertyRule propertyRule);
}
