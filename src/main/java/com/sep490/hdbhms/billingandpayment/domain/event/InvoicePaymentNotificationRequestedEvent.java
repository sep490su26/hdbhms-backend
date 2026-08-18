package com.sep490.hdbhms.billingandpayment.domain.event;

import java.util.Map;

public record InvoicePaymentNotificationRequestedEvent(
        String eventType,
        Long invoiceId,
        Long recipientUserId,
        String recipientEmail,
        String recipientPhone,
        String subject,
        String body,
        Map<String, Object> payload
) {
}
