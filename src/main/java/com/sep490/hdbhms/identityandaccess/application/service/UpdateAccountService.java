package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.*;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.UpdateAccountUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.AccountModificationHistoryRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodePort;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.model.UserModificationHistory;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.AccountStatus;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.ModificationType;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.OtpType;
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

import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UpdateAccountService implements UpdateAccountUseCase {
    AccountModificationHistoryRepository accountModificationHistoryRepository;
    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    TokenProvider tokenProvider;

    OtpCodePort otpCodePort;

//    @Qualifier("usernameBloomFilter")
//    RBloomFilter<String> usernameBloomFilter;
//    @Qualifier("emailBloomFilter")
//    RBloomFilter<String> emailBloomFilter;

    @Override
    public User updateAccountStatus(UpdateAccountStatusCommand command) {
        var account = userRepository.findById(command.id())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var oldStatus = account.getStatus();
        AccountStatus newStatus;
        try {
            newStatus = AccountStatus.valueOf(command.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ApiErrorCode.UNDEFINED);
        }
        account.changeStatus(newStatus);
        account = userRepository.save(account);
        var modificationHistory = UserModificationHistory.newAccountModificationHistory(
                account.getId(),
                ModificationType.STATUS,
                oldStatus.getValue(),
                newStatus.getValue()
        );
        accountModificationHistoryRepository.save(modificationHistory);
        return account;
    }

    @Override
    public User updateAccountRole(UpdateAccountRoleCommand command) {
        var account = userRepository.findById(command.id())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var oldRole = account.getRole();
        var newRole = command.newRole();
        account.assignRole(newRole);
        account = userRepository.save(account);
        var modificationHistory = UserModificationHistory.newAccountModificationHistory(
                account.getId(),
                ModificationType.ROLE,
                oldRole.toString(),
                newRole.toString()
        );
        accountModificationHistoryRepository.save(modificationHistory);
        return account;
    }


    @Override
    public void requestUpdateAccountEmail(UpdateAccountEmailCommand command) {
        if (userRepository.existsByEmail(command.newEmail())) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        var account = userRepository.findById(command.id())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var lastEmailChangedInstant = accountModificationHistoryRepository
                .getLatestUsernameModificationInstant(account.getId());
        if (lastEmailChangedInstant != null && InstantUtils.isFixedUnitsAgoFromNow(
                lastEmailChangedInstant,
                48,
                ChronoUnit.HOURS)
        ) {
            throw new AppException(ApiErrorCode.CHANGE_USERNAME_NOT_ALLOWED_YET);
        }
        if (!passwordEncoder.matches(command.currentPassword(), account.getPasswordHash())) {
            throw new AppException(ApiErrorCode.INVALID_CREDENTIALS);
        }
        otpCodePort.sendOtp(account.getId(), command.newEmail(), OtpType.EMAIL_MODIFICATION, command.newEmail());
    }


    @Override
    public User confirmUpdateAccountEmail(VerifyUpdateEmailCommand command) {
        var account = command.user();
        var newEmail = otpCodePort.verifyOtp(account.getId(), OtpType.EMAIL_MODIFICATION, command.otp());
        if (userRepository.existsByEmail(newEmail)) {
            throw new AppException(ApiErrorCode.ACCOUNT_EXISTED);
        }
        var oldEmail = account.getEmail();
        var lastEmailChangedInstant = accountModificationHistoryRepository
                .getLatestUsernameModificationInstant(account.getId());
        account.changeEmail(newEmail, lastEmailChangedInstant);
        account = userRepository.save(account);

        var modificationHistory = UserModificationHistory.newAccountModificationHistory(
                account.getId(),
                ModificationType.EMAIL,
                oldEmail,
                newEmail
        );
        accountModificationHistoryRepository.save(modificationHistory);

        invalidateAllSession(account, command.request(), command.response());
//        emailBloomFilter.add(account.getEmail());
        return account;
    }

    @Override
    public User updateAccountPassword(UpdateAccountPasswordCommand command) {
        var account = command.user();
        if (!passwordEncoder.matches(command.currentPassword(), account.getPasswordHash())) {
            throw new AppException(ApiErrorCode.INVALID_CREDENTIALS);
        }
        if (passwordEncoder.matches(command.newPassword(), account.getPasswordHash())) {
            throw new AppException(ApiErrorCode.SAME_PASSWORD);
        }
        var latestPasswordChanges = accountModificationHistoryRepository
                .getTopLatestPasswordChangeValue(account.getId(), 3);

        latestPasswordChanges.forEach(passwordChange -> {
            if (passwordEncoder.matches(command.newPassword(), passwordChange)) {
                throw new AppException(ApiErrorCode.OLD_PASSWORD);
            }
        });

        var oldPasswordHash = account.getPasswordHash();
        var newPasswordHash = passwordEncoder.encode(command.newPassword());
        account.changePassword(newPasswordHash);
        account = userRepository.save(account);
        var modificationHistory = UserModificationHistory.newAccountModificationHistory(
                account.getId(),
                ModificationType.PASSWORD_CHANGE,
                oldPasswordHash,
                newPasswordHash
        );
        accountModificationHistoryRepository.save(modificationHistory);
        invalidateAllSessionExceptCurrent(account, command.request(), command.response());
        return account;
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
