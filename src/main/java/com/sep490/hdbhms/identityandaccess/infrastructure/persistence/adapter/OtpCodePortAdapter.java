package com.sep490.hdbhms.identityandaccess.infrastructure.persistence.adapter;

import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodeGenerator;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpCodePort;
import com.sep490.hdbhms.identityandaccess.application.port.out.OtpEmailSender;
import com.sep490.hdbhms.identityandaccess.domain.value_objects.OtpType;
import com.sep490.hdbhms.shared.exception.ApiErrorCode;
import com.sep490.hdbhms.shared.exception.AppException;
import com.sep490.hdbhms.shared.utils.StringUtils;
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
public class OtpCodePortAdapter implements OtpCodePort {
    OtpEmailSender otpEmailSender;
    OtpCodeGenerator otpCodeGenerator;
    RedisTemplate<String, String> redisTemplate;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    @Override
    public void sendOtp(Long accountId, String toEmail, OtpType type, String payload) {
        var otp = otpCodeGenerator.generate();
        var key = buildKey(accountId, type, otp);

        // Store a placeholder if payload is null
        var valueToStore = !StringUtils.isEmpty(payload) ? payload : "OK";
        redisTemplate.opsForValue().set(key, valueToStore, OTP_TTL.toMillis(), TimeUnit.MILLISECONDS);
        otpEmailSender.sendOTPEmail(toEmail, otp);
        log.info("OTP sent to {} for type {}", toEmail, type);
    }

    @Override
    public String verifyOtp(Long accountId, OtpType type, String otp) {
        var key = buildKey(accountId, type, otp);
        var storedValue = redisTemplate.opsForValue().getAndDelete(key);
        if (storedValue == null) {
            throw new AppException(ApiErrorCode.OTP_CODE_EXPIRED_OR_MISMATCH);
        }
        if ("OK".equals(storedValue)) {
            return null;
        }
        return storedValue;
    }

    private String buildKey(Long accountId, OtpType type, String otp) {
        return String.format(
                "otp:%s:%s:%s",
                accountId,
                StringUtils.toSlugUnderscore(type.name().toLowerCase()),
                otp
        );
    }
}
