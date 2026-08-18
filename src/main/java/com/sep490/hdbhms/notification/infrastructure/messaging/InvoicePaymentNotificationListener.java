package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.billingandpayment.domain.event.InvoicePaymentNotificationRequestedEvent;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.application.service.NotificationTemplateManagementService;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.processor.NotificationOutboxProcessor;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InvoicePaymentNotificationListener {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    NotificationOutboxRepository notificationOutboxRepository;
    NotificationTemplateManagementService notificationTemplateManagementService;
    NotificationOutboxProcessor notificationOutboxProcessor;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailExecutor")
    public void handle(InvoicePaymentNotificationRequestedEvent event) {
        NotificationChannel channel = resolvePreferredChannel(event.recipientEmail(), event.recipientPhone());
        if (channel == null) {
            log.warn("Skipping invoice payment notification because the tenant has no valid contact information. invoiceId={}, recipientUserId={}",
                    event.invoiceId(), event.recipientUserId());
            return;
        }

        NotificationOutbox outbox = buildOutbox(event, channel);
        if (outbox == null) {
            return;
        }

        NotificationOutbox savedOutbox = notificationOutboxRepository.save(outbox);
        notificationOutboxProcessor.process(savedOutbox.getId());
    }

    private NotificationOutbox buildOutbox(
            InvoicePaymentNotificationRequestedEvent event,
            NotificationChannel channel
    ) {
        String eventType = event.eventType() == null || event.eventType().isBlank()
                ? "INVOICE_PAYMENT_SUCCESS"
                : event.eventType();
        Map<String, Object> payload = buildPayload(event);
        NotificationTemplateManagementService.PreviewResult rendered =
                notificationTemplateManagementService.preview(
                                eventType,
                                channel,
                                null,
                                null,
                                payload
                        )
                        .orElse(null);

        return NotificationOutbox.builder()
                .eventType(eventType)
                .targetType("INVOICE")
                .targetId(event.invoiceId())
                .recipientUserId(event.recipientUserId())
                .recipientEmail(channel == NotificationChannel.EMAIL ? event.recipientEmail() : null)
                .recipientPhone(channel == NotificationChannel.SMS ? event.recipientPhone() : null)
                .channel(channel)
                .title(rendered == null ? event.subject() : rendered.title())
                .body(rendered == null ? event.body() : rendered.body())
                .payload(toJson(payload))
                .status(OutboxStatus.PENDING)
                .maxRetries(channel == NotificationChannel.SMS ? 1 : 3)
                .isRead(false)
                .scheduledAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private NotificationChannel resolvePreferredChannel(String email, String phone) {
        if (isValidEmail(email)) {
            return NotificationChannel.EMAIL;
        }
        if (phone != null && !phone.trim().isEmpty()) {
            return NotificationChannel.SMS;
        }
        return null;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String normalized = email.trim().toLowerCase();
        return !normalized.endsWith("@tenant.hdbhms.local")
                && !normalized.endsWith("tenant.hdbhms.local")
                && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private Map<String, Object> buildPayload(InvoicePaymentNotificationRequestedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (event.payload() != null) {
            payload.putAll(event.payload());
        }
        payload.put("invoiceId", event.invoiceId());
        payload.put("eventType", event.eventType());
        payload.put("recipientUserId", event.recipientUserId());
        payload.put("recipientEmail", event.recipientEmail());
        payload.put("recipientPhone", event.recipientPhone());
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize invoice payment notification data. invoiceId={}",
                    payload.get("invoiceId"), exception);
            return null;
        }
    }
}
