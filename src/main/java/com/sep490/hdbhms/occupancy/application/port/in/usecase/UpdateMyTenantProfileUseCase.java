package com.sep490.hdbhms.occupancy.application.port.in.usecase;

import com.sep490.hdbhms.occupancy.infrastructure.web.dto.request.UpdateTenantProfileRequest;
import com.sep490.hdbhms.occupancy.infrastructure.web.dto.response.TenantProfileResponse;

public interface UpdateMyTenantProfileUseCase {
    TenantProfileResponse execute(UpdateTenantProfileRequest request);
}
