package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.identityandaccess.domain.event.PreCreatedAccountNotificationRequestedEvent;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PreCreatedAccountNotificationListener {
    NotificationOutboxRepository notificationOutboxRepository;
    NotificationOutboxProcessor notificationOutboxProcessor;
    ObjectMapper objectMapper;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailExecutor")
    public void handle(PreCreatedAccountNotificationRequestedEvent event) {
        if (event.preferredChannel() == null) {
            return;
        }

        switch (event.preferredChannel()) {
            case EMAIL -> saveAndDispatch(buildEmailOutbox(event));
            case SMS -> saveAndDispatch(buildSmsOutbox(event));
            default -> log.warn(
                    "Unsupported pre-created account notification channel. contractId={}, recipientProfileId={}, channel={}",
                    event.contractId(),
                    event.recipientProfileId(),
                    event.preferredChannel()
            );
        }
    }

    private NotificationOutbox buildEmailOutbox(PreCreatedAccountNotificationRequestedEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            return null;
        }

        return NotificationOutbox.builder()
                .eventType("PRE_CREATED_ACCOUNT_NOTIFICATION")
                .targetType("TENANT_ACCOUNT_PROVISIONING")
                .targetId(event.recipientProfileId())
                .recipientUserId(event.recipientUserId())
                .recipientEmail(event.email())
                .channel(NotificationChannel.EMAIL)
                .title(event.subject())
                .body(event.body())
                .payload(buildPayload(event))
                .status(OutboxStatus.PENDING)
                .maxRetries(3)
                .isRead(false)
                .scheduledAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private NotificationOutbox buildSmsOutbox(PreCreatedAccountNotificationRequestedEvent event) {
        if (event.phone() == null || event.phone().isBlank()) {
            return null;
        }

        return NotificationOutbox.builder()
                .eventType("PRE_CREATED_ACCOUNT_NOTIFICATION")
                .targetType("TENANT_ACCOUNT_PROVISIONING")
                .targetId(event.recipientProfileId())
                .recipientUserId(event.recipientUserId())
                .recipientPhone(event.phone())
                .channel(NotificationChannel.SMS)
                .title(event.subject())
                .body(event.body())
                .payload(buildPayload(event))
                .status(OutboxStatus.PENDING)
                .maxRetries(1)
                .isRead(false)
                .scheduledAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void saveAndDispatch(NotificationOutbox outbox) {
        if (outbox == null) {
            return;
        }

        NotificationOutbox savedOutbox = notificationOutboxRepository.save(outbox);
        notificationOutboxProcessor.process(savedOutbox.getId());
    }

    private String buildPayload(PreCreatedAccountNotificationRequestedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contractId", event.contractId());
        payload.put("recipientProfileId", event.recipientProfileId());
        payload.put("recipientEmail", event.email());
        payload.put("recipientPhone", event.phone());
        payload.put("batch", event.batch());

        if (event.credentials() != null && !event.credentials().isEmpty()) {
            payload.put(
                    "tenantProfileIds",
                    event.credentials().stream()
                            .map(credential -> credential.tenantProfileId())
                            .collect(Collectors.toList())
            );
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            log.warn(
                    "Failed to serialize pre-created account payload. contractId={}, recipientProfileId={}",
                    event.contractId(),
                    event.recipientProfileId(),
                    exception
            );
            return null;
        }
    }
}
