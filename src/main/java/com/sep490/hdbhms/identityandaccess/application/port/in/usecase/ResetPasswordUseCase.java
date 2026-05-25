package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.RequestResetPasswordCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.ResetPasswordCommand;

public interface ResetPasswordUseCase {
    void requestResetPassword(RequestResetPasswordCommand command);

    void resetPassword(ResetPasswordCommand command);
}
