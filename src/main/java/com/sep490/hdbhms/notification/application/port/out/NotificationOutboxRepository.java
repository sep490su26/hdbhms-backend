package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {
    NotificationOutbox save(NotificationOutbox notificationOutbox);

    Optional<NotificationOutbox> findById(Long id);

    List<NotificationOutbox> findReadyPending(LocalDateTime now, int limit);

    org.springframework.data.domain.Page<NotificationOutbox> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(Long userId, NotificationChannel channel, org.springframework.data.domain.Pageable pageable);

    List<NotificationOutbox> findNextNotificationsCursor(Long userId, NotificationChannel channel, long after, int limit);

    default List<NotificationOutbox> findNextNotificationsCursor(
            Long userId,
            NotificationChannel channel,
            long after,
            int limit,
            Long roomId
    ) {
        return findNextNotificationsCursor(userId, channel, after, limit);
    }

    default List<NotificationOutbox> findNextNotificationsCursor(
            Long userId,
            NotificationChannel channel,
            long after,
            int limit,
            Long roomId,
            String roomCode
    ) {
        return findNextNotificationsCursor(userId, channel, after, limit, roomId);
    }

    long countByRecipientUserIdAndChannelAndIsReadFalse(Long userId, NotificationChannel channel);

    default long countByRecipientUserIdAndChannelAndIsReadFalse(
            Long userId,
            NotificationChannel channel,
            Long roomId,
            String roomCode
    ) {
        return countByRecipientUserIdAndChannelAndIsReadFalse(userId, channel);
    }

    void markAllAsRead(Long userId, NotificationChannel channel);
    void markAllAsRead(Long userId, NotificationChannel channel, LocalDateTime readAt);

    default void markAllAsRead(
            Long userId,
            NotificationChannel channel,
            Long roomId,
            LocalDateTime readAt
    ) {
        markAllAsRead(userId, channel, readAt);
    }

    default void markAllAsRead(
            Long userId,
            NotificationChannel channel,
            Long roomId,
            String roomCode,
            LocalDateTime readAt
    ) {
        markAllAsRead(userId, channel, roomId, readAt);
    }
    void markAllAsRead(Long userId, LocalDateTime readAt);

    void markTargetAsRead(Long userId, String targetType, Long targetId, LocalDateTime readAt);

    boolean markAsProcessing(Long id);
}
