package com.sep490.hdbhms.identityandaccess.application.port.out;

public interface PasswordResetEmailVerifierSender {
    void sendResetPasswordVerifier(Long userId, String email, String phone, String passwordResetCode, String resetLink);
}
