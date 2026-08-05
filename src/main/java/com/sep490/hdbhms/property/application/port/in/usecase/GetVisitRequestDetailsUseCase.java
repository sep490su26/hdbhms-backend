package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetVisitRequestDetailsQuery;
import com.sep490.hdbhms.property.domain.model.VisitRequest;

public interface GetVisitRequestDetailsUseCase {
    VisitRequest execute(GetVisitRequestDetailsQuery query);
}
