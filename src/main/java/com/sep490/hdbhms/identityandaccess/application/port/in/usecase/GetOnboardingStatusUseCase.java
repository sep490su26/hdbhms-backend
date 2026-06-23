package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetResidentOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetStaffOnboardingStatusQuery;
import com.sep490.hdbhms.identityandaccess.infrastructure.web.dto.response.OnboardingStatusResponse;

public interface GetOnboardingStatusUseCase {
    OnboardingStatusResponse ofResident(GetResidentOnboardingStatusQuery query);

    OnboardingStatusResponse ofStaff(GetStaffOnboardingStatusQuery query);
}
