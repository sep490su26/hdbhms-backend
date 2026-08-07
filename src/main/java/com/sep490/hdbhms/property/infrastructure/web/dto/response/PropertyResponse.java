package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import com.sep490.hdbhms.property.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.property.domain.value_objects.PropertyType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PropertyResponse {
    Long id;
    String propertyCode;
    String name;
    PropertyType propertyType;
    String addressLine;
    String description;
    PropertyStatus status;
    List<PropertyImageResponse> images;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;
}
