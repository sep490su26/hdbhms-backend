package com.sep490.hdbhms.property.application.port.in.usecase;

import com.sep490.hdbhms.property.application.port.in.query.GetListVisitRequestsQuery;
import com.sep490.hdbhms.property.domain.model.VisitRequest;
import org.springframework.data.domain.Page;

public interface GetListVisitRequestsUseCase {
    Page<VisitRequest> execute(GetListVisitRequestsQuery query);
}
