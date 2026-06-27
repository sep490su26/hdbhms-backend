package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;

public interface NotificationDeliveryRepository {
    NotificationDelivery save(NotificationDelivery notificationDelivery);
}
