package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationEventConsumer {
    SendNotificationUseCase sendNotificationUseCase;

    @KafkaListener(topics = {
        "notification-events"
    }, groupId = "hdbhms-notification-group")
    public void handle(NotificationEvent event) {
        sendNotificationUseCase.queueNotification(event);
    }
}
