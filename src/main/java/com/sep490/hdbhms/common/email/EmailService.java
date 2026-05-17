package com.sep490.hdbhms.common.email;

public interface EmailService {

    void sendTemporaryAccount(String to, String loginId, String temporaryPassword);
}
