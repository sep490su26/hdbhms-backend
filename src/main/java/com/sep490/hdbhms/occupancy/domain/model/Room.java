package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {
    final Long id;
    Long propertyId;
    Long floorId;
    String roomCode;
    String name;
    BigDecimal areaM2;

    @Builder.Default
    Long listedPrice = 0L;
    @Builder.Default
    RoomStatus currentStatus = RoomStatus.VACANT;
    @Builder.Default
    Integer maxOccupants = 3;

    String publicNote;
    String internalNote;

    Integer positionX;
    Integer positionY;

    @Builder.Default
    Integer sortOrder = 0;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime deletedAt;

    @Builder.Default
    Long version = 0L;


    public static Room newRoom(
            Long propertyId,
            Long floorId,
            String roomCode,
            String name,
            BigDecimal areaM2,
            Long listedPrice,
            Integer maxOccupants,
            Integer sortOrder
    ) {
        return Room.builder()
                .propertyId(propertyId)
                .floorId(floorId)
                .roomCode(roomCode)
                .name(name)
                .areaM2(areaM2)
                .listedPrice(listedPrice)
                .maxOccupants(maxOccupants)
                .sortOrder(sortOrder)
                .build();
    }

    public void reserveRoom() {
        this.currentStatus = RoomStatus.RESERVED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reserveRoomForTransfer() {
        this.currentStatus = RoomStatus.RESERVED_FOR_TRANSFER;
        this.updatedAt = LocalDateTime.now();
    }

    public void holdRoom() {
        this.currentStatus = RoomStatus.ON_HOLD;
        this.updatedAt = LocalDateTime.now();
    }

    public void releaseRoom() {
        this.currentStatus = RoomStatus.VACANT;
        this.updatedAt = LocalDateTime.now();
    }

    public void occupyRoom() {
        this.currentStatus = RoomStatus.OCCUPIED;
        this.updatedAt = LocalDateTime.now();
    }
}
