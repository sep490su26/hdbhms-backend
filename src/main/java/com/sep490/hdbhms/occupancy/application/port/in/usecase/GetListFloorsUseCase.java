package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListFloorsQuery;
import com.sep490.hdbhms.occupancy.domain.model.Floor;

import java.util.List;

public interface GetListFloorsUseCase {
    List<Floor> execute(GetListFloorsQuery query);
}
