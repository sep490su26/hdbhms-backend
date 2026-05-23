package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateFloorRequest {
    Long propertyId;
    String floorCode;
    String name;
    Integer sortOrder;
}
