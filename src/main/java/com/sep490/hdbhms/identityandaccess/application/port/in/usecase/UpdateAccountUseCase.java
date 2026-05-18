package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.domain.model.User;

public interface UpdateAccountUseCase {
    User updateAccountStatus(UpdateAccountStatusCommand command);

    User updateAccountRole(UpdateAccountRoleCommand command);

    void requestUpdateAccountEmail(UpdateAccountEmailCommand command);

    User confirmUpdateAccountEmail(VerifyUpdateEmailCommand command);

    User updateAccountPassword(UpdateAccountPasswordCommand command);
}
