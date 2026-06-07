package com.sep490.hdbhms.notification.application.port.out;

import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import java.util.List;

public interface NotificationTemplateRepository {
    NotificationTemplate save(NotificationTemplate notificationTemplate);
    List<NotificationTemplate> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status);
}
