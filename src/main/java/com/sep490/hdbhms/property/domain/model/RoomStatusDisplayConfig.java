package com.sep490.hdbhms.property.domain.model;

import com.sep490.hdbhms.property.domain.value_objects.RoomStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomStatusDisplayConfig {
    final Long id;
    RoomStatus roomStatus;
    String colorHex;
    String label;
}
