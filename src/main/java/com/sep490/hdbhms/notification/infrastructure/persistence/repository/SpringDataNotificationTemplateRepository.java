package com.sep490.hdbhms.notification.infrastructure.persistence.repository;

import com.sep490.hdbhms.notification.application.port.out.NotificationTemplateRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.TemplateStatus;
import com.sep490.hdbhms.notification.infrastructure.persistence.jpa.JpaNotificationTemplateRepository;
import com.sep490.hdbhms.notification.infrastructure.persistence.mapper.NotificationTemplatePersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataNotificationTemplateRepository implements NotificationTemplateRepository {
    JpaNotificationTemplateRepository jpaRepository;
    NotificationTemplatePersistenceMapper mapper;

    @Override
    public NotificationTemplate save(NotificationTemplate notificationTemplate) {
        var entity = mapper.toEntity(notificationTemplate);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public List<NotificationTemplate> findByTemplateKeyAndStatus(String templateKey, TemplateStatus status) {
        return jpaRepository.findByTemplateKeyAndStatus(templateKey, status)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<NotificationTemplate> findByTemplateKeyAndChannel(String templateKey, NotificationChannel channel) {
        return jpaRepository.findByTemplateKeyAndChannel(templateKey, channel)
                .map(mapper::toDomain);
    }
}
