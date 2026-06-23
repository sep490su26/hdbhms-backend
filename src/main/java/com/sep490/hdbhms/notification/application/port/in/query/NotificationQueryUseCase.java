package com.sep490.hdbhms.notification.application.port.in.query;

import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationQueryUseCase {
    Page<NotificationOutbox> getNotificationsWeb(Long userId, NotificationChannel channel, Pageable pageable);

    List<NotificationOutbox> getNotificationsMobile(Long userId, NotificationChannel channel, long after, int limit);

    long getUnreadCount(Long userId);
}
