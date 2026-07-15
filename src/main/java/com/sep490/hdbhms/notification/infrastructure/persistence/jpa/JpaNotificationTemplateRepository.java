package com.sep490.hdbhms.notification.infrastructure.persistence.jpa;

import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaNotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Long> {
    List<NotificationTemplateEntity> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status);
    Optional<NotificationTemplateEntity> findByTemplateKeyAndChannel(String templateKey, NotificationChannel channel);
}
