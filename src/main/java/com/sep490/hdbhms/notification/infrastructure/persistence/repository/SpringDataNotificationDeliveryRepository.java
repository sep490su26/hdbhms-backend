package com.sep490.hdbhms.notification.infrastructure.persistence.repository;

import com.sep490.hdbhms.notification.application.port.out.NotificationDeliveryRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataNotificationDeliveryRepository implements NotificationDeliveryRepository {
    @Override
    public NotificationDelivery save(NotificationDelivery notificationDelivery) {
        return null;
    }
}
