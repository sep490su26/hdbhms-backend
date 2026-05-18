package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.RefreshAccessTokenCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface RefreshAccessTokenUseCase {
    Authentication execute(RefreshAccessTokenCommand command, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse);
}
