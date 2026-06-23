package com.sep490.hdbhms.notification.infrastructure.persistence.mapper;

import com.sep490.hdbhms.notification.domain.model.NotificationTemplate;
import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationTemplateEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplatePersistenceMapper {

    public NotificationTemplate toDomain(NotificationTemplateEntity entity) {
        if (entity == null) return null;
        return NotificationTemplate.builder()
                .id(entity.getId())
                .templateKey(entity.getTemplateKey())
                .channel(entity.getChannel())
                .titleTemplate(entity.getTitleTemplate())
                .bodyTemplate(entity.getBodyTemplate())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationTemplateEntity toEntity(NotificationTemplate domain) {
        if (domain == null) return null;
        return NotificationTemplateEntity.builder()
                .id(domain.getId())
                .templateKey(domain.getTemplateKey())
                .channel(domain.getChannel())
                .titleTemplate(domain.getTitleTemplate())
                .bodyTemplate(domain.getBodyTemplate())
                .status(domain.getStatus())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
