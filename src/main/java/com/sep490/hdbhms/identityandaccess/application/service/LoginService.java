package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.LoginCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.LoginUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.LoginHistoryRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.Authentication;
import com.sep490.hdbhms.identityandaccess.domain.model.WebAuthentication;
import com.sep490.hdbhms.identityandaccess.domain.model.LoginHistory;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.TokenProvider;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.HttpUtils;
import com.sep490.hdbhms.shared.utils.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LoginService implements LoginUseCase {
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    TokenProvider tokenProvider;
    LoginHistoryRepository loginHistoryRepository;

    @Override
    public Authentication execute(String clientType, LoginCommand command, HttpServletRequest request, HttpServletResponse response) {
        log.info(command.phone());
        User user = userRepository.findByPhone(command.phone())
                .orElseThrow(() -> new AppException(ApiErrorCode.UNDEFINED));
        boolean passwordMatched = passwordEncoder.matches(
                command.password(),
                user.getPasswordHash()
        );

        if (!passwordMatched) {
            LoginHistory loginHistory = LoginHistory.newAccountModificationHistory(
                    user.getId(),
                    LoginStatus.INVALID_PASSWORD,
                    HttpUtils.getClientIpAddress(request),
                    HttpUtils.getUserAgent(request),
                    LoginMethod.PASSWORD,
                    null,
                    SessionUtils.getOrCreateDeviceId(request, response)
            );
            loginHistoryRepository.save(loginHistory);
            throw new AppException(ApiErrorCode.INVALID_PASSWORD);
        }

        if ("web".equals(clientType) && !isStaff(user)) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        else if ("mobile".equals(clientType) && isStaff(user)) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }

        String sessionId = tokenProvider.createRefreshToken(user, request, response);
        String accessToken = tokenProvider.createAccessToken(user, sessionId, response);
        LoginHistory loginHistory = LoginHistory.newAccountModificationHistory(
                user.getId(),
                LoginStatus.SUCCESS,
                HttpUtils.getClientIpAddress(request),
                HttpUtils.getUserAgent(request),
                LoginMethod.PASSWORD,
                sessionId,
                SessionUtils.getOrCreateDeviceId(request, response)
        );
        loginHistoryRepository.save(loginHistory);
        return new WebAuthentication(accessToken, true);
    }

    private boolean isStaff(User user) {
        return user.getRole() == Role.OWNER
                || user.getRole() == Role.MANAGER
                || user.getRole() == Role.ACCOUNTANT;
    }
}
