package com.sep490.hdbhms.identityverification.application.port.in.usecase;

import com.sep490.hdbhms.identityverification.application.port.in.command.UploadIdentityVerificationCommand;
import com.sep490.hdbhms.identityverification.infrastructure.web.dto.response.IdentityVerificationResponse;

public interface UploadIdentityVerificationUseCase {
    IdentityVerificationResponse execute(UploadIdentityVerificationCommand command);
}
