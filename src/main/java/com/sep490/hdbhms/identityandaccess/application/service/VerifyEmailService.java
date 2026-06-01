package com.sep490.hdbhms.identityandaccess.application.service;

import com.sep490.hdbhms.identityandaccess.application.port.in.command.RequestVerifyEmailCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.command.VerifyEmailCommand;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.VerifyEmailUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.UserRepository;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodePort;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.OtpType;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VerifyEmailService implements VerifyEmailUseCase {
    OtpCodePort otpCodePort;
    UserRepository userRepository;

    @Override
    public void requestEmailVerification(RequestVerifyEmailCommand command) {
        var account = command.user();
        if (account.isMustChangePassword()) {
            throw new AppException(ApiErrorCode.ACCOUNT_IS_ALREADY_VERIFIED);
        }
        otpCodePort.sendOtp(
                command.user().getId(),
                command.user().getEmail(),
                OtpType.EMAIL_VERIFICATION,
                null
        );
    }

    @Override
    public void verifyEmail(VerifyEmailCommand command) {
        var account = command.user();
        if (account.isMustChangePassword()) {
            throw new AppException(ApiErrorCode.ACCOUNT_IS_ALREADY_VERIFIED);
        }
        otpCodePort.verifyOtp(
                command.user().getId(),
                OtpType.EMAIL_VERIFICATION,
                command.otp()
        );
        userRepository.save(account);
    }
}
