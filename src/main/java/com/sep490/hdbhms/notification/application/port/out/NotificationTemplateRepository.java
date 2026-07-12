package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository {
    NotificationTemplate save(NotificationTemplate notificationTemplate);
    List<NotificationTemplate> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status);
    Optional<NotificationTemplate> findByTemplateKeyAndChannel(String templateKey, NotificationChannel channel);
}
