package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;

import java.time.LocalDateTime;

public interface NotificationDeliveryRepository {
    NotificationDelivery save(NotificationDelivery notificationDelivery);

    void markReadByOutboxId(Long outboxId, LocalDateTime readAt);

    void markReadByRecipientUserId(Long userId, LocalDateTime readAt);

    void markReadByRecipientUserIdAndTarget(Long userId, String targetType, Long targetId, LocalDateTime readAt);
}
