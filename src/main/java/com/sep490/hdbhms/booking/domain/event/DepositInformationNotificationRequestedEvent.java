package com.sep490.hdbhms.booking.domain.event;

import java.util.Map;

public record DepositInformationNotificationRequestedEvent(
        Long depositAgreementId,
        Long depositBatchId,
        Long recipientUserId,
        String recipientEmail,
        String recipientFullName,
        String recipientPhone,
        String subject,
        String body,
        Map<String, Object> payload
) {
}
