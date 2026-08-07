package com.sep490.hdbhms.property.infrastructure.web.mapper;

import com.sep490.hdbhms.property.domain.model.PropertyImage;
import com.sep490.hdbhms.property.infrastructure.web.dto.response.PropertyImageResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PropertyImageWebMapper {
    @Mapping(target = "url", expression = "java(resolveUrl(propertyImage))")
    PropertyImageResponse toResponse(PropertyImage propertyImage);

    default String resolveUrl(PropertyImage propertyImage) {
        if (propertyImage == null || propertyImage.getFileId() == null) {
            return null;
        }
        return "/api/v1/files/download/" + propertyImage.getFileId();
    }
}
