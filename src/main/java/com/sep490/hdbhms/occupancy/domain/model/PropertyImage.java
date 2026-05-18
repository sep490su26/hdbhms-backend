package com.sep490.hdbhms.occupancy.domain.model;

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
public class PropertyImage {
    final Long id;
    Long propertyId;
    Long fileId;
    Integer sortOrder;
    LocalDateTime createdAt;
}
