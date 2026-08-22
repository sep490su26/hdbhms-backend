package com.sep490.hdbhms.notification.application.port.in.query;

import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationQueryUseCase {
    Page<NotificationOutbox> getNotificationsWeb(Long userId, NotificationChannel channel, Pageable pageable);

    List<NotificationOutbox> getNotificationsMobile(Long userId, NotificationChannel channel, long after, int limit);

    default List<NotificationOutbox> getNotificationsMobile(
            Long userId,
            NotificationChannel channel,
            long after,
            int limit,
            Long roomId
    ) {
        return getNotificationsMobile(userId, channel, after, limit);
    }

    default List<NotificationOutbox> getNotificationsMobile(
            Long userId,
            NotificationChannel channel,
            long after,
            int limit,
            Long roomId,
            String roomCode
    ) {
        return getNotificationsMobile(userId, channel, after, limit, roomId);
    }

    long getUnreadCount(Long userId, NotificationChannel channel);

    default long getUnreadCount(
            Long userId,
            NotificationChannel channel,
            Long roomId,
            String roomCode
    ) {
        return getUnreadCount(userId, channel);
    }
}
