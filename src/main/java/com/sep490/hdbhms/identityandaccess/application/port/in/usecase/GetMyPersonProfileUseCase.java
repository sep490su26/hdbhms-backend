package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;

public interface GetMyPersonProfileUseCase {
    PersonProfile execute();
}
