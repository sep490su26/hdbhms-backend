package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.occupancy.domain.model.RoomImage;

import java.util.List;

public interface GetRoomImagesByRoomIdUseCase {
    List<RoomImage> execute(GetRoomImagesByRoomIdQuery query);
}
