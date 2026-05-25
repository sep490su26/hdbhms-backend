package com.sep490.hdbhms.occupancy.infrastructure.web.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateRoomRequest {
    Long propertyId;
    Long floorId;
    String roomCode;
    String name;
    BigDecimal areaM2;
    Long listedPrice;
    Integer maxOccupants;
    Integer sortOrder;
}
