package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.domain.model.User;

public interface UpdateUserUseCase {
    User updateUserStatus(UpdateAccountStatusCommand command);

    User updateUserRole(UpdateAccountRoleCommand command);

    void requestUpdateUserEmail(UpdateAccountEmailCommand command);

    User confirmUpdateUserEmail(VerifyUpdateEmailCommand command);

    User updateUserPassword(UpdateUserPasswordCommand command);
}
