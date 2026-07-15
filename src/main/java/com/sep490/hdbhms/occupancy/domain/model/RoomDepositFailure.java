package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomDepositFailureReason;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoomDepositFailure {
    Long id;
    Long roomId;
    Long roomHoldId;
    Long paymentIntentId;
    RoomDepositFailureReason reason;
    LocalDateTime occurredAt;
    LocalDateTime createdAt;
}
