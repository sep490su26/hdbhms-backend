package com.sep490.hdbhms.identityandaccess.application.port.in.usecase;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.LoginCommand;
import com.sep490.hdbhms.identityandaccess.domain.model.Authentication;
import com.sep490.hdbhms.identityandaccess.domain.model.WebAuthentication;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface LoginUseCase {
    Authentication execute(String clientType, LoginCommand command, HttpServletRequest request, HttpServletResponse response);
}
