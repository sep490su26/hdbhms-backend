package com.sep490.hdbhms.notification.application.port.in.usecase;

import com.sep490.hdbhms.notification.domain.valueObjects.NotificationChannel;

public interface ManageNotificationUseCase {
    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId, NotificationChannel channel);
}
