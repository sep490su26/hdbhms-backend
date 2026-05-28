package com.sep490.hdbhms.portal.application.port.in.usecase;

import com.sep490.hdbhms.portal.application.port.in.query.GetHomeQuery;
import com.sep490.hdbhms.portal.infrastructure.web.dto.response.HomeResponse;

public interface GetHomeUseCase {
    HomeResponse execute(GetHomeQuery query);
}
