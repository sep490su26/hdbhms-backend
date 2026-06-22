package com.sep490.hdbhms.notification.application.port.in.usecase;

public interface ManageNotificationUseCase {
    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);
}
