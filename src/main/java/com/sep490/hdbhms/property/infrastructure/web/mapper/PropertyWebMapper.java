package com.sep490.hdbhms.property.infrastructure.web.mapper;

import com.sep490.hdbhms.property.domain.model.Property;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.PropertyResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PropertyWebMapper {
    PropertyResponse toResponse(Property property);
}
