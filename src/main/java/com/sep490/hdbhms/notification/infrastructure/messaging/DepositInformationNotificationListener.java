package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.booking.domain.event.DepositInformationNotificationRequestedEvent;
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
public class DepositInformationNotificationListener {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    NotificationOutboxRepository notificationOutboxRepository;
    NotificationTemplateManagementService notificationTemplateManagementService;
    NotificationOutboxProcessor notificationOutboxProcessor;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailExecutor")
    public void handle(DepositInformationNotificationRequestedEvent event) {
        NotificationChannel channel = resolvePreferredChannel(event.recipientEmail(), event.recipientPhone());
        if (channel == null) {
            log.warn("Skipping deposit notification because the customer has no valid contact information. agreementId={}, batchId={}",
                    event.depositAgreementId(), event.depositBatchId());
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
            DepositInformationNotificationRequestedEvent event,
            NotificationChannel channel
    ) {
        Long targetId = event.depositAgreementId() != null
                ? event.depositAgreementId()
                : event.depositBatchId();
        String targetType = event.depositAgreementId() != null
                ? "DEPOSIT_AGREEMENT"
                : "DEPOSIT_BATCH";
        Map<String, Object> payload = buildPayload(event);
        NotificationTemplateManagementService.PreviewResult rendered =
                notificationTemplateManagementService.preview(
                                "DEPOSIT_INFORMATION_NOTIFICATION",
                                channel,
                                null,
                                null,
                                payload
                        )
                        .orElse(null);

        return NotificationOutbox.builder()
                .eventType("DEPOSIT_INFORMATION_NOTIFICATION")
                .targetType(targetType)
                .targetId(targetId)
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

    private Map<String, Object> buildPayload(DepositInformationNotificationRequestedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (event.payload() != null) {
            payload.putAll(event.payload());
        }
        payload.put("recipientFullName", event.recipientFullName());
        payload.put("depositAgreementId", event.depositAgreementId());
        payload.put("depositBatchId", event.depositBatchId());
        payload.put("recipientEmail", event.recipientEmail());
        payload.put("recipientPhone", event.recipientPhone());

        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize deposit notification data", exception);
            return null;
        }
    }
}
