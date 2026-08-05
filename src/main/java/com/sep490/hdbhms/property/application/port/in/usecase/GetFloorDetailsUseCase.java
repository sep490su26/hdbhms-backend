package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetFloorDetailsQuery;
import com.sep490.hdbhms.property.domain.model.Floor;

public interface GetFloorDetailsUseCase {
    Floor execute(GetFloorDetailsQuery query);
}
