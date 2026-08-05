package com.sep490.hdbhms.property.infrastructure.web.dto.response;

import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomSimpleResponse {
    Long id;
    String roomCode;
    String name;
    Long propertyId;
    RoomStatus status;
    Long listedPrice;
}
