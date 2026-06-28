package com.sep490.hdbhms.identityandaccess.domain.event;

import com.sep490.hdbhms.identityandaccess.application.port.out.SendPreCreatedAccountPort;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;

import java.util.List;

public record PreCreatedAccountNotificationRequestedEvent(
        Long contractId,
        Long recipientProfileId,
        Long recipientUserId,
        String email,
        String recipientFullName,
        String phone,
        NotificationChannel preferredChannel,
        String subject,
        String body,
        boolean batch,
        List<SendPreCreatedAccountPort.AccountCredential> credentials
) {
}
