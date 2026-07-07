package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomStatusHistory {
    Long id;
    Long roomId;
    RoomStatus fromStatus;
    RoomStatus toStatus;
    String reason;
    Long changedById;
    LocalDateTime changedAt;
}
