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

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SendEmailPreCreatedAccountAdapter {
    NotificationOutboxRepository notificationOutboxRepository;

    @EventListener
    @Async("emailExecutor")
    public void handle(PreCreatedAccountNotificationRequestedEvent event) {
        if (event.preferredChannel() != NotificationChannel.EMAIL) {
            return;
        }

        if (event.email() == null || event.email().isBlank()) {
            return;
        }

        notificationOutboxRepository.save(NotificationOutbox.builder()
                .eventType("PRE_CREATED_ACCOUNT_NOTIFICATION")
                .targetType("USER_ACCOUNT")
                .recipientEmail(event.email())
                .channel(NotificationChannel.EMAIL)
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