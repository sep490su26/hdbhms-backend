package com.sep490.hdbhms.identityandaccess.application.port.out;

public interface SendPreCreatedAccountPort {
    void sendAccountInformation(String email, String fullName, String phone, String randomPassword);
}
