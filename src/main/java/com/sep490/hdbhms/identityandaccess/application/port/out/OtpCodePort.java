package com.sep490.hdbhms.identityandaccess.application.port.out;

import com.sep490.hdbhms.identityandaccess.domain.valueObjects.OtpType;

public interface OtpCodePort {

    void sendOtp(Long accountId, String toEmail, OtpType type, String payload);

    String verifyOtp(Long accountId, OtpType type, String otp);
}
