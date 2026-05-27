package com.sep490.hdbhms.portal.application.port.in.usecase;

import com.sep490.hdbhms.portal.application.port.in.query.GetDashboardQuery;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.DashboardResponse;

public interface GetDashboardUseCase {
    DashboardResponse execute(GetDashboardQuery query);
}
