package com.sep490.hdbhms.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationEventConsumer {
    SendNotificationUseCase sendNotificationUseCase;
    ObjectMapper objectMapper;

    @KafkaListener(topics = {
        "notification-events"
    }, groupId = "hdbhms-notification-group")
    public void handle(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            sendNotificationUseCase.queueNotification(event);
        } catch (Exception exception) {
            log.error("Failed to consume notification event payload={}", payload, exception);
        }
    }
}
