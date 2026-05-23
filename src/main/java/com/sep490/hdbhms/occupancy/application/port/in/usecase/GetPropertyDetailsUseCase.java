package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetPropertyDetailsQuery;
import com.sep490.hdbhms.occupancy.domain.model.Property;

public interface GetPropertyDetailsUseCase {
    Property execute(GetPropertyDetailsQuery query);
}
