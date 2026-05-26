package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.RefreshAccessTokenCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.RefreshAccessTokenUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.WebAuthentication;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.TokenProvider;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.CookieUtils;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;

import static com.sep490.hdbhms.shared.utils.SessionUtils.SESSION_ID_COOKIE_NAME;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RefreshAccessTokenService implements RefreshAccessTokenUseCase {
    TokenProvider tokenProvider;
    UserRepository userRepository;

    @Override
    public WebAuthentication execute(RefreshAccessTokenCommand command, HttpServletRequest request, HttpServletResponse response) {
        var refreshToken = tokenProvider.getRefreshToken(request, true);
        log.info("Refresh token: {}", refreshToken);
        try {
            tokenProvider.verifyToken(refreshToken, true);
        } catch (AppException ignored) {
            log.info("Refresh token expired: {}", refreshToken);
            throw new AppException(ApiErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (ParseException | JOSEException e) {
            throw new AppException(ApiErrorCode.INVALID_JWT_TOKEN);
        }

        var accessToken = command.token();
        tokenProvider.clearToken(accessToken, false);
        var accountId = tokenProvider.getUserId(request);
        var account = userRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var sessionCookie = CookieUtils.getCookie(request, SESSION_ID_COOKIE_NAME)
                .orElseThrow(() -> new AppException(ApiErrorCode.UNAUTHENTICATED));
        var newAccessToken = tokenProvider.createAccessToken(
                account,
                sessionCookie.getValue(),
                response
        );
        return new WebAuthentication(newAccessToken, true);
    }
}
