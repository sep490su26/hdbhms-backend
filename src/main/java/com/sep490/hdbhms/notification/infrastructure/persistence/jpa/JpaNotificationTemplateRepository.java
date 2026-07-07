package com.sep490.hdbhms.notification.infrastructure.persistence.jpa;

import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.sep490.hdbhms.notification.domain.valueObjects.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JpaNotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {
    List<NotificationTemplateEntity> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status);
}
