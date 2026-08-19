package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.UpdateMyPersonProfileCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.PersonProfile;

public interface UpdateMyPersonProfileUseCase {
    PersonProfile execute(UpdateMyPersonProfileCommand command);
}
