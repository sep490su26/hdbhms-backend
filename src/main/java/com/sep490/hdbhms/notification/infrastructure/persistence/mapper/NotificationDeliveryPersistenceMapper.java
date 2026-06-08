package com.sep490.hdbhms.notification.infrastructure.persistence.mapper;

import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;
import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationDeliveryEntity;
import com.sep490.hdbhms.notification.infrastructure.persistence.jpa.JpaNotificationOutboxRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationDeliveryPersistenceMapper {

    JpaNotificationOutboxRepository jpaNotificationOutboxRepository;

    public NotificationDelivery toDomain(NotificationDeliveryEntity entity) {
        if (entity == null) return null;
        return NotificationDelivery.builder()
                .id(entity.getId())
                .outboxId(entity.getOutbox() != null ? entity.getOutbox().getId() : null)
                .providerMessageId(entity.getProviderMessageId())
                .deliveryStatus(entity.getDeliveryStatus())
                .errorMessage(entity.getErrorMessage())
                .deliveredAt(entity.getDeliveredAt())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public NotificationDeliveryEntity toEntity(NotificationDelivery domain) {
        if (domain == null) return null;
        return NotificationDeliveryEntity.builder()
                .id(domain.getId())
                .outbox(domain.getOutboxId() != null
                        ? jpaNotificationOutboxRepository.getReferenceById(domain.getOutboxId())
                        : null)
                .providerMessageId(domain.getProviderMessageId())
                .deliveryStatus(domain.getDeliveryStatus())
                .errorMessage(domain.getErrorMessage())
                .deliveredAt(domain.getDeliveredAt())
                .readAt(domain.getReadAt())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}