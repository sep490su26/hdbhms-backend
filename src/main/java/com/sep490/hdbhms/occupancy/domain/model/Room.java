package com.sep490.hdbhms.occupancy.domain.model;

import com.sep490.hdbhms.occupancy.domain.value_objects.RoomStatus;
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
}
