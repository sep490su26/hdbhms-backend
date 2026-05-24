package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.occupancy.domain.model.VisitRequest;
import org.springframework.data.domain.Page;

public interface GetListVisitRequestsUseCase {
    Page<VisitRequest> execute(GetListVisitRequestsQuery query);
}
