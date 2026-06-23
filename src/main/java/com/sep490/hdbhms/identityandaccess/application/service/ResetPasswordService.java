package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.RequestResetPasswordCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.ResetPasswordCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.ResetPasswordUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserModificationHistoryRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetTokenPort;
import com.sep490.hdbhms.identityandaccess.domain.model.UserModificationHistory;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.ModificationType;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ResetPasswordService implements ResetPasswordUseCase {
    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    PasswordResetTokenPort passwordResetTokenPort;
    UserModificationHistoryRepository userModificationHistoryRepository;

    @Override
    public void requestResetPassword(RequestResetPasswordCommand command) {
        var account = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND));
        var numberOfPasswordResetOfTheLast3Days = userModificationHistoryRepository.getNumberOfPasswordResetOfTheDays(account.getId(), 3);
        if (numberOfPasswordResetOfTheLast3Days >= 10) {
            throw new AppException(ApiErrorCode.RESET_PASSWORD_NOT_ALLOWED_YET);
        }
        passwordResetTokenPort.sendPasswordResetToken(account.getId(), account.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordCommand command) {
        if (!passwordResetTokenPort.hasToken(command.token())) {
            throw new AppException(ApiErrorCode.RESET_PASSWORD_TOKEN_EXPIRED);
        }
        var accountId = passwordResetTokenPort.getAccountIdByToken(command.token());
        var account = userRepository.findById(accountId)
                .orElseThrow(
                        () -> new AppException(ApiErrorCode.ACCOUNT_NOT_FOUND)
                );
        if (!Objects.equals(account.getId(), accountId)) {
            throw new AppException(ApiErrorCode.RESET_PASSWORD_TOKEN_MISMATCH);
        }
        var oldPasswordHash = account.getPasswordHash();
        var newPasswordHash = passwordEncoder.encode(command.newPassword());
        account.changePassword(newPasswordHash);
        account = userRepository.save(account);
        var modificationHistory = UserModificationHistory.newUserModificationHistory(
                account.getId(),
                ModificationType.PASSWORD_RESET,
                oldPasswordHash,
                newPasswordHash
        );
        userModificationHistoryRepository.save(modificationHistory);
        passwordResetTokenPort.deleteToken(command.token());
    }
}
