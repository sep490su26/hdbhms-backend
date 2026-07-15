package com.sep490.hdbhms.notification.application.port.in.usecase;

import com.sep490.hdbhms.shared.event.NotificationEvent;

public interface SendNotificationUseCase {
    void queueNotification(NotificationEvent event);
}
