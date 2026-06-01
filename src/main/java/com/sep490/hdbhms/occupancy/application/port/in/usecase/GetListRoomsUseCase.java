package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListRoomsQuery;
import com.sep490.hdbhms.occupancy.domain.model.Room;
import org.springframework.data.domain.Page;

public interface GetListRoomsUseCase {
    Page<Room> execute(GetListRoomsQuery query);
}
