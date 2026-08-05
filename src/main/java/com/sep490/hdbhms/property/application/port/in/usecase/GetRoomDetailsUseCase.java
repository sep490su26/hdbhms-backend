package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetRoomDetailsQuery;
import com.sep490.hdbhms.property.domain.model.Room;

public interface GetRoomDetailsUseCase {
    Room execute(GetRoomDetailsQuery query);
}
