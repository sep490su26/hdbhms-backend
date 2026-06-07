package com.sep490.hdbhms.notification.infrastructure.persistence.jpa;

import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaNotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
}
