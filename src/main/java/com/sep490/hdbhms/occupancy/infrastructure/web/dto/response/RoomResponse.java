package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
    Long propertyId;
    Long floorId;
    Long listedPrice;
    BigDecimal areaM2;
    Integer maxOccupants;
    RoomStatus currentStatus;
    Integer positionX;
    Integer positionY;
    LocalDate expectedVacantDate;
    String firstImageUrl;
    List<RoomImageResponse> images;
}
