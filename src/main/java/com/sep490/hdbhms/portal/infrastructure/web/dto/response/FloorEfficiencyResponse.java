package com.sep490.hdbhms.portal.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FloorEfficiencyResponse {
    Long propertyId;
    String propertyName;
    Long floorId;
    String floorName;
    Long roomCount;
    Long vacantRoomCount;
}
