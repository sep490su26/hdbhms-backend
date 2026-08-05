package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetRoomImagesByRoomIdQuery;
import com.sep490.hdbhms.property.domain.model.RoomImage;

import java.util.List;

public interface GetRoomImagesByRoomIdUseCase {
    List<RoomImage> execute(GetRoomImagesByRoomIdQuery query);
}
