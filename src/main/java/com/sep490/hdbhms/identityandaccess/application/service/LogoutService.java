package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.LogoutCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.LogoutUseCase;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.TokenProvider;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LogoutService implements LogoutUseCase {
    TokenProvider tokenProvider;

    @Override
    public void execute(LogoutCommand command, HttpServletRequest request, HttpServletResponse response) {
        try {
            var refreshToken = tokenProvider.getRefreshToken(request);
            if (!StringUtils.isEmpty(refreshToken)) {
                tokenProvider.clearToken(refreshToken, true);
            }
            var accessToken = command.token();
            if (!StringUtils.isEmpty(accessToken)) {
                tokenProvider.clearToken(accessToken, false);
            }
            tokenProvider.clearSession(request, response);
            SecurityContextHolder.clearContext();
        } catch (AppException ex) {
            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
        }
    }
}
