package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.in.query.GetAccountByIdQuery;
import com.sep490.hdbhms.identityandaccess.application.port.in.usecase.GetUserUseCase;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodeGenerator;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetEmailVerifierSender;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetTokenGenerator;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetTokenPort;
import com.sep490.hdbhms.identityandaccess.domain.model.User;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Role;
import com.sep490.hdbhms.identityandaccess.infrastructure.config.ResetPasswordConfig;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PasswordResetTokenAdapter implements PasswordResetTokenPort {
    GetUserUseCase getUserUseCase;
    OtpCodeGenerator otpCodeGenerator;
    ResetPasswordConfig resetPasswordConfig;
    RedisTemplate<String, String> redisTemplate;
    PasswordResetTokenGenerator passwordResetTokenGenerator;
    private static final Duration OTP_TTL = Duration.ofMinutes(15);
    PasswordResetEmailVerifierSender passwordResetEmailVerifierSender;

    @Override
    public void sendPasswordResetToken(Long userId, String toEmail) {
        User user = getUserUseCase.getById(new GetAccountByIdQuery(userId));
        String passwordResetCode = otpCodeGenerator.generate();
        String key = buildKey(passwordResetCode);
        redisTemplate.opsForValue().set(key, String.valueOf(userId), OTP_TTL.toMillis(), TimeUnit.MILLISECONDS);
        String resetLink = String.format(
                "%s?code=%s",
                (user.getRole() == Role.TENANT || user.getRole() == Role.LEAD) ?
                        resetPasswordConfig.getMobileConfirmationUrl()
                        : resetPasswordConfig.getWebConfirmationUrl(),
                passwordResetCode
        );
        passwordResetEmailVerifierSender.sendResetPasswordVerifier(toEmail, passwordResetCode, resetLink);
    }

    @Override
    public boolean hasToken(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(token)));
    }

    @Override
    public Long getAccountIdByToken(String token) {
        var key = buildKey(token);
        var value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void deleteToken(String token) {
        redisTemplate.delete(buildKey(token));
    }

    private String buildKey(String token) {
        return String.format(
                "password_reset_token:%s",
                token
        );
    }
}
