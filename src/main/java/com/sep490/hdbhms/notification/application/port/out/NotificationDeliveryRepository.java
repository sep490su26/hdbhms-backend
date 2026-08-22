package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;

import java.time.LocalDateTime;

public interface NotificationDeliveryRepository {
    NotificationDelivery save(NotificationDelivery notificationDelivery);

    void markReadByOutboxId(Long outboxId, LocalDateTime readAt);

    void markReadByRecipientUserId(Long userId, LocalDateTime readAt);

    void markReadByRecipientUserIdAndChannel(Long userId, NotificationChannel channel, LocalDateTime readAt);

    default void markReadByRecipientUserIdAndChannel(
            Long userId,
            NotificationChannel channel,
            Long roomId,
            LocalDateTime readAt
    ) {
        markReadByRecipientUserIdAndChannel(userId, channel, readAt);
    }

    default void markReadByRecipientUserIdAndChannel(
            Long userId,
            NotificationChannel channel,
            Long roomId,
            String roomCode,
            LocalDateTime readAt
    ) {
        markReadByRecipientUserIdAndChannel(userId, channel, roomId, readAt);
    }

    void markReadByRecipientUserIdAndTarget(Long userId, String targetType, Long targetId, LocalDateTime readAt);
}
