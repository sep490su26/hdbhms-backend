package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.LoginCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.LoginUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.LoginHistoryRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.*;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginMethod;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.LoginStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.TokenProvider;
import com.sep490.hdbhms.occupancy.application.port.out.TenantRepository;
import com.sep490.hdbhms.occupancy.domain.model.Tenant;
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

import java.util.Locale;

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
    TenantRepository tenantRepository;
    TenantAccountProvisioningStatusService provisioningStatusService;

    @Override
    public Authentication execute(String clientType, LoginCommand command, HttpServletRequest request, HttpServletResponse response) {
        String normalizedClientType = clientType == null ? "" : clientType.toLowerCase(Locale.ROOT);
        User user = userRepository.findByPhoneOrEmailAndDeletedAtIsNull(command.phone(), command.phone())
                .orElseThrow(() -> new AppException(ApiErrorCode.INVALID_CREDENTIALS));
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
            throw new AppException(ApiErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(ApiErrorCode.ACCOUNT_IS_NOT_ACTIVE);
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
        provisioningStatusService.markActiveByUserId(user.getId());
        if ("web".equals(normalizedClientType) && isStaff(user)) {
            return new WebAuthentication(accessToken, user.getRole(), true);
        } else if ("mobile".equals(normalizedClientType) && !isStaff(user)) {
            Tenant tenant = tenantRepository.findByUserId(user.getId()).orElse(null);
            return new MobileAuthentication(
                    accessToken,
                    sessionId,
                    user.getRole(),
                    tenant == null ? null : tenant.getId(),
                    tenant == null ? null : tenant.getPropertyId(),
                    true
            );
        } else {
            throw new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND);
        }
    }

    private boolean isStaff(User user) {
        return user.getRole() == Role.OWNER
                || user.getRole() == Role.MANAGER
                || user.getRole() == Role.ACCOUNTANT;
    }
}
