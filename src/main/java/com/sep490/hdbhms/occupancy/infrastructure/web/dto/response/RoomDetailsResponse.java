package com.sep490.hdbhms.occupancy.infrastructure.web.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomDetailsResponse {
    Long id;
    String roomCode;
    String name;
    BigDecimal areaM2;
    Long listedPrice;
    String currentStatus;
    String publicNote;
    Integer maxOccupants;
    Integer positionX;
    Integer positionY;
    Integer sortOrder;

    FloorResponse floor;
    List<RoomImageResponse> images;
}

