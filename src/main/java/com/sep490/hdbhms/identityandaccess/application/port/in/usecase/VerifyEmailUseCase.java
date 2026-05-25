package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.RequestVerifyEmailCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.VerifyEmailCommand;

public interface VerifyEmailUseCase {
    void requestEmailVerification(RequestVerifyEmailCommand command);

    void verifyEmail(VerifyEmailCommand command);
}
