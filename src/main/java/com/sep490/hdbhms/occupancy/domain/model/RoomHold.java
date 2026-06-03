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

    public static RoomHold createRoomHoldForGuest(
            Long roomId,
            LocalDateTime expiresAt
    ) {
        return RoomHold.builder()
                .roomId(roomId)
                .expiresAt(expiresAt)
                .build();
    }

    public void releaseOnAutoExpired() {
        if (
                this.getStatus() != RoomHoldStatus.ACTIVE
                        && this.getStatus() != RoomHoldStatus.PAYMENT_PROCESSING
                        || this.getExpiresAt().isAfter(LocalDateTime.now())
        ) {
            throw new IllegalStateException("Scheduled task is not expired yet");
        }
        this.status = RoomHoldStatus.EXPIRED;
        this.releasedAt = LocalDateTime.now();
    }

    public void confirm() {
        if (this.getStatus() != RoomHoldStatus.ACTIVE) {
            throw new IllegalStateException("Scheduled task is not active");
        }
        this.status = RoomHoldStatus.CONFIRMED;
    }

    public void confirmPaidHold() {
        if (this.status == RoomHoldStatus.CONFIRMED) {
            return;
        }
        this.status = RoomHoldStatus.CONFIRMED;
        this.releasedAt = null;
    }

    public void cancel() {
        if (this.status == RoomHoldStatus.CONFIRMED) {
            throw new IllegalStateException("Room hold is already confirmed");
        }
        if (this.status == RoomHoldStatus.CANCELLED || this.status == RoomHoldStatus.EXPIRED) {
            return;
        }
        this.status = RoomHoldStatus.CANCELLED;
        this.releasedAt = LocalDateTime.now();
    }
}
