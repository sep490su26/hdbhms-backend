package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyStatus;
import com.sep490.hdbhms.occupancy.domain.value_objects.PropertyType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Property {
    final Long id;
    String propertyCode;
    String name;
    @Builder.Default
    PropertyType propertyType = PropertyType.BOARDING_HOUSE;
    String addressLine;
    String description;
    @Builder.Default
    PropertyStatus status = PropertyStatus.ACTIVE;
    final LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    public static Property newProperty(
            String propertyCode,
            String name,
            PropertyType type) {
        return Property.builder()
                .build();
    }
}
