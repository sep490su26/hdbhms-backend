package com.sep490.hdbhms.occupancy.infrastructure.web.mapper;

import com.sep490.hdbhms.occupancy.domain.model.Property;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.PropertyResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PropertyWebMapper {
    PropertyResponse toResponse(Property property);
}
