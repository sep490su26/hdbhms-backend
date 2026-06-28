package com.sep490.hdbhms.identityandaccess.application.port.out;

import java.util.List;

public interface SendPreCreatedAccountPort {
    void sendAccountInformation(
            Long contractId,
            Long tenantProfileId,
            Long recipientUserId,
            String email,
            String fullName,
            String phone,
            String randomPassword
    );

    void sendAccountInformationBatch(
            Long contractId,
            Long recipientProfileId,
            Long recipientUserId,
            String email,
            String recipientFullName,
            String phone,
            List<AccountCredential> credentials
    );

    record AccountCredential(
            Long tenantProfileId,
            String fullName,
            String phone,
            String randomPassword,
            String roomRole
    ) {
    }
}