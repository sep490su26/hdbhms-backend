package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.occupancy.domain.model.Floor;

public interface GetFloorDetailsUseCase {
    Floor execute(GetFloorDetailsQuery query);
}
