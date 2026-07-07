package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.FloorStatus;
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
public class Floor {
    final Long id;
    Long propertyId;
    String floorCode;
    String name;
    Integer sortOrder;
    @Builder.Default
    FloorStatus status = FloorStatus.ACTIVE;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    public static Floor newFloor(
            Long propertyId,
            String floorCode,
            String name,
            Integer sortOrder
    ) {
        return Floor.builder()
                .propertyId(propertyId)
                .floorCode(floorCode)
                .name(name)
                .sortOrder(sortOrder)
                .status(FloorStatus.ACTIVE)
                .build();
    }
}
