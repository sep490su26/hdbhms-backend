package com.sep490.hdbhms.identityandaccess.application.port.out;

import java.util.List;

public interface SendPreCreatedAccountPort {
    void sendAccountInformation(String email, String fullName, String phone, String randomPassword);

    void sendAccountInformationBatch(String email, String recipientFullName, List<AccountCredential> credentials);

    record AccountCredential(
            String fullName,
            String phone,
            String randomPassword,
            String roomRole
    ) {
    }
}
