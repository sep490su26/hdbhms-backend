package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.UpdateUserUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodePort;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserModificationHistoryRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.model.UserModificationHistory;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.ModificationType;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.OtpType;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.security.TokenProvider;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.AuthUtils;
import com.sep490.hdbhms.shared.utils.InstantUtils;
import com.sep490.hdbhms.shared.utils.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateUserService implements UpdateUserUseCase {
    UserModificationHistoryRepository userModificationHistoryRepository;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    TokenProvider tokenProvider;
    TenantAccountProvisioningStatusService provisioningStatusService;

    OtpCodePort otpCodePort;

//    @Qualifier("usernameBloomFilter")
//    RBloomFilter<String> usernameBloomFilter;
//    @Qualifier("emailBloomFilter")
//    RBloomFilter<String> emailBloomFilter;

    @Override
    public User updateUserStatus(UpdateAccountStatusCommand command) {
        var account = userRepository.findById(command.id())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var oldStatus = account.getStatus();
        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(command.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND);
        }
        account.changeStatus(newStatus);
        account = userRepository.save(account);
        var modificationHistory = UserModificationHistory.newUserModificationHistory(
                account.getId(),
                ModificationType.STATUS,
                oldStatus.getValue(),
                newStatus.getValue()
        );
        userModificationHistoryRepository.save(modificationHistory);
        return account;
    }

    @Override
    public User updateUserRole(UpdateAccountRoleCommand command) {
        var account = userRepository.findById(command.id())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var oldRole = account.getRole();
        var newRole = command.newRole();
        account.assignRole(newRole);
        account = userRepository.save(account);
        var modificationHistory = UserModificationHistory.newUserModificationHistory(
                account.getId(),
                ModificationType.ROLE,
                oldRole.toString(),
                newRole.toString()
        );
        userModificationHistoryRepository.save(modificationHistory);
        return account;
    }


    @Override
    public void requestUpdateUserEmail(UpdateAccountEmailCommand command) {
        if (userRepository.existsByEmail(command.newEmail())) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        var user = userRepository.findById(command.id())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var lastEmailChangedInstant = userModificationHistoryRepository
                .getLatestUsernameModificationInstant(user.getId());
        if (lastEmailChangedInstant != null && InstantUtils.isFixedUnitsAgoFromNow(
                lastEmailChangedInstant,
                48,
                ChronoUnit.HOURS)
        ) {
            throw new AppException(ApiErrorCode.CHANGE_USERNAME_NOT_ALLOWED_YET);
        }
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ApiErrorCode.INVALID_CREDENTIALS);
        }
        otpCodePort.sendOtp(user.getId(), command.newEmail(), OtpType.EMAIL_MODIFICATION, command.newEmail());
    }


    @Override
    public User confirmUpdateUserEmail(VerifyUpdateEmailCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        String newEmail = otpCodePort.verifyOtp(user.getId(), OtpType.EMAIL_MODIFICATION, command.otp());
        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        String oldEmail = user.getEmail();
        Instant lastEmailChangedInstant = userModificationHistoryRepository
                .getLatestUsernameModificationInstant(user.getId());
        user.changeEmail(newEmail, lastEmailChangedInstant);
        user = userRepository.save(user);

        UserModificationHistory modificationHistory = UserModificationHistory.newUserModificationHistory(
                user.getId(),
                ModificationType.EMAIL,
                oldEmail,
                newEmail
        );
        userModificationHistoryRepository.save(modificationHistory);

        invalidateAllSession(user, command.request(), command.response());
//        emailBloomFilter.add(account.getEmail());
        return user;
    }

    @Override
    public User updateUserPassword(UpdateUserPasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new AppException(ApiErrorCode.INVALID_CREDENTIALS);
        }
        if (passwordEncoder.matches(command.newPassword(), user.getPasswordHash())) {
            throw new AppException(ApiErrorCode.SAME_PASSWORD);
        }
        Set<String> latestPasswordChanges = userModificationHistoryRepository
                .getTopLatestPasswordChangeValue(user.getId(), 3);

        latestPasswordChanges.forEach(passwordChange -> {
            if (passwordEncoder.matches(command.newPassword(), passwordChange)) {
                throw new AppException(ApiErrorCode.OLD_PASSWORD);
            }
        });

        String oldPasswordHash = user.getPasswordHash();
        String newPasswordHash = passwordEncoder.encode(command.newPassword());
        user.changePassword(newPasswordHash);
        user = userRepository.save(user);
        UserModificationHistory modificationHistory = UserModificationHistory
                .newUserModificationHistory(
                        user.getId(),
                        ModificationType.PASSWORD_CHANGE,
                        oldPasswordHash,
                        newPasswordHash
                );
        userModificationHistoryRepository.save(modificationHistory);
        invalidateAllSessionExceptCurrent(user, command.request(), command.response());
        return user;
    }

    @Override
    public User updateUserFirstPassword(UpdateUserFirstPasswordCommand command) {
        User user = userRepository.findById(command.userId())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        if (!user.isMustChangePassword()) {
            throw new AppException(ApiErrorCode.INVALID_CREDENTIALS);
        }
        String oldPasswordHash = user.getPasswordHash();
        String newPasswordHash = passwordEncoder.encode(command.newPassword());
        user.changePassword(newPasswordHash);
        user.registerFirstPasswordChange();
        user = userRepository.save(user);
        UserModificationHistory modificationHistory = UserModificationHistory
                .newUserModificationHistory(
                        user.getId(),
                        ModificationType.PASSWORD_CHANGE,
                        oldPasswordHash,
                        newPasswordHash
                );
        userModificationHistoryRepository.save(modificationHistory);
        provisioningStatusService.markActiveByUserId(user.getId());
        return user;
    }

    private void invalidateAllSession(User user, HttpServletRequest request, HttpServletResponse response) {
        var refreshToken = tokenProvider.getRefreshToken(request);
        if (!StringUtils.isEmpty(refreshToken)) {
            tokenProvider.clearToken(refreshToken, true);
        }
        var accessToken = AuthUtils.extractToken();
        if (!StringUtils.isEmpty(accessToken)) {
            tokenProvider.clearToken(accessToken, false);
        }
        tokenProvider.clearAllUserSessions(user.getId(), request, response);
        SecurityContextHolder.clearContext();
    }

    private void invalidateAllSessionExceptCurrent(User user, HttpServletRequest request, HttpServletResponse response) {
        tokenProvider.clearAllUserSessionsExceptCurrent(user.getId(), request, response);
        SecurityContextHolder.clearContext();
    }
}
