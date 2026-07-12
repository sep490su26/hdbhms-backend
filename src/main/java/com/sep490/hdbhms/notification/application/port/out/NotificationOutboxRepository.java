package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository {
    NotificationOutbox save(NotificationOutbox notificationOutbox);

    Optional<NotificationOutbox> findById(Long id);

    List<NotificationOutbox> findByStatusAndNextRetryAtBefore(OutboxStatus outboxStatus, LocalDateTime localDateTime);

    org.springframework.data.domain.Page<NotificationOutbox> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(Long userId, NotificationChannel channel, org.springframework.data.domain.Pageable pageable);

    List<NotificationOutbox> findNextNotificationsCursor(Long userId, NotificationChannel channel, long after, int limit);

    long countByRecipientUserIdAndChannelAndIsReadFalse(Long userId, NotificationChannel channel);

    void markAllAsRead(Long userId, NotificationChannel channel);
    void markAllAsRead(Long userId, LocalDateTime readAt);

    void markTargetAsRead(Long userId, String targetType, Long targetId, LocalDateTime readAt);

    boolean markAsProcessing(Long id);
}
