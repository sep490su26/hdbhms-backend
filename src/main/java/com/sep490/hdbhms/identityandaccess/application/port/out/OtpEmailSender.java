package com.sep490.hdbhms.identityandaccess.application.port.out;

public interface OtpEmailSender {
    void sendOTPEmail(String email, String otpCode);
}
