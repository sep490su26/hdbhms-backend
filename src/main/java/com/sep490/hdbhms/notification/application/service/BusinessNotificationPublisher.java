package com.sep490.hdbhms.notification.application.service;

import com.sep490.hdbhms.notification.application.port.in.usecase.SendNotificationUseCase;
import com.sep490.hdbhms.shared.event.NotificationEvent;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BusinessNotificationPublisher {
    SendNotificationUseCase sendNotificationUseCase;

    public void publish(
            String eventType,
            Long recipientUserId,
            String targetType,
            Long targetId,
            Map<String, Object> data
    ) {
        if (eventType == null || eventType.isBlank() || recipientUserId == null) {
            return;
        }
        sendNotificationUseCase.queueNotification(NotificationEvent.builder()
                .eventType(eventType.trim())
                .userId(recipientUserId)
                .targetType(targetType)
                .targetId(targetId)
                .data(data == null ? Map.<String, Object>of() : new LinkedHashMap<>(data))
                .build());
    }
}
