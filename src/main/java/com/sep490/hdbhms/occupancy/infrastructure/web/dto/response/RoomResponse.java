package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomResponse {
    Long id;
    String roomCode;
    String name;
    String floorName;
    String propertyName;
    Long listedPrice;
    BigDecimal areaM2;
    Integer maxOccupants;
    RoomStatus currentStatus;
    String firstImageUrl;
}
