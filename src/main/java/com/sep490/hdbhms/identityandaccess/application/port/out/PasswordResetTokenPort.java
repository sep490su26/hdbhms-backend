package com.sep490.hdbhms.identityandaccess.application.port.out;

public interface PasswordResetTokenPort {
    void sendPasswordResetToken(Long accountId, String toEmail);

    boolean hasToken(String token);

    Long getAccountIdByToken(String token);

    void deleteToken(String token);
}
