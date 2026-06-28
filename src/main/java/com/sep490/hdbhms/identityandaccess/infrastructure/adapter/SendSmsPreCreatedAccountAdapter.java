package com.sep490.hdbhms.identityandaccess.infrastructure.adapter;

import com.sep490.hdbhms.identityandaccess.domain.event.PreCreatedAccountNotificationRequestedEvent;
import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Identity-domain sms projection for pre-created account notifications.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendSmsPreCreatedAccountAdapter {
    NotificationOutboxRepository notificationOutboxRepository;

    @EventListener
    @Async("smsExecutor")
    public void handle(PreCreatedAccountNotificationRequestedEvent event) {
        if (event.preferredChannel() != NotificationChannel.SMS) {
            return;
        }

        if (event.phone() == null || event.phone().isBlank()) {
            return;
        }

        notificationOutboxRepository.save(NotificationOutbox.builder()
                .eventType("PRE_CREATED_ACCOUNT_NOTIFICATION")
                .targetType("USER_ACCOUNT")
                .recipientPhone(event.phone())
                .channel(NotificationChannel.SMS)
                .title(event.subject())
                .body(event.body())
                .status(OutboxStatus.PENDING)
                .maxRetries(3)
                .isRead(false)
                .scheduledAt(LocalDateTime.now())
                .nextRetryAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }
}