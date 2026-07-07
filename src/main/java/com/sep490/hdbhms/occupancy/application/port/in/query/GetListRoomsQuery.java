package com.sep490.hdbhms.occupancy.application.port.in.query;

import com.sep490.hdbhms.occupancy.domain.valueObjects.RoomStatus;
import org.springframework.data.domain.Pageable;

public record GetListRoomsQuery(
        Long propertyId,
        Long floorId,
        RoomStatus status,
        Long minPrice,
        Long maxPrice,
        Pageable pageable
) {
}
