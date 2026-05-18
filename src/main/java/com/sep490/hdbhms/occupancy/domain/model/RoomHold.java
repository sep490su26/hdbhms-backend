package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomHoldStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomHold {
    Long id;
    Long roomId;
    Long tenantId;
    @Builder.Default
    RoomHoldStatus status = RoomHoldStatus.ACTIVE;
    LocalDateTime expiresAt;
    LocalDateTime createdAt;
    LocalDateTime releasedAt;
    Long activeRoomKey;
}
