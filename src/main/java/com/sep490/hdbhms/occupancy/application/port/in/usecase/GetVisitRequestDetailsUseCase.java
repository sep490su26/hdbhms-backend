package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;

public interface GetVisitRequestDetailsUseCase {
    VisitRequest execute(GetVisitRequestDetailsQuery query);
}
