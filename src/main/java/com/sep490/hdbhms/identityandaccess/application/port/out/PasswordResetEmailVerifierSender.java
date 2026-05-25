package com.sep490.hdbhms.identityandaccess.application.port.out;

public interface PasswordResetEmailVerifierSender {
    void sendResetPasswordVerifier(String email, String resetLink);
}
