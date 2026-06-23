package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TenantProfileResponse;

public interface GetMyTenantProfileUseCase {
    TenantProfileResponse execute();
}
