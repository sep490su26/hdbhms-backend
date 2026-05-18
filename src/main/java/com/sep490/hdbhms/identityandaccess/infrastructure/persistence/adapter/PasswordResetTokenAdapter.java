package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetEmailVerifierSender;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetTokenGenerator;
import com.sep490.hdbhms.identityandaccess.application.port.out.PasswordResetTokenPort;
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
    ResetPasswordConfig resetPasswordConfig;
    RedisTemplate<String, String> redisTemplate;
    PasswordResetTokenGenerator passwordResetTokenGenerator;
    PasswordResetEmailVerifierSender passwordResetEmailVerifierSender;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    @Override
    public void sendPasswordResetToken(Long accountId, String toEmail) {
        var passwordResetToken = passwordResetTokenGenerator.generate();
        var key = buildKey(passwordResetToken);
        redisTemplate.opsForValue().set(key, String.valueOf(accountId), OTP_TTL.toMillis(), TimeUnit.MILLISECONDS);
        var resetLink = String.format(
                "%s?token=%s",
                resetPasswordConfig.getResetPasswordConfirmationUrl(),
                passwordResetToken
        );
        passwordResetEmailVerifierSender.sendResetPasswordVerifier(toEmail, resetLink);
        log.info("Password reset token sent to {}", toEmail);
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
