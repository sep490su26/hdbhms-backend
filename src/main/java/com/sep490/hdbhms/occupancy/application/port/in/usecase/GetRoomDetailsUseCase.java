package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.occupancy.domain.model.Room;

public interface GetRoomDetailsUseCase {
    Room execute(GetRoomDetailsQuery query);
}
