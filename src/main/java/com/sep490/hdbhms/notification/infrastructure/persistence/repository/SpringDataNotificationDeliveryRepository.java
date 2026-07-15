package com.sep490.hdbhms.notification.infrastructure.persistence.repository;

import com.sep490.hdbhms.notification.application.port.out.NotificationDeliveryRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationDelivery;
import com.sep490.hdbhms.notification.infrastructure.persistence.jpa.JpaNotificationDeliveryRepository;
import com.sep490.hdbhms.notification.infrastructure.persistence.mapper.NotificationDeliveryPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataNotificationDeliveryRepository implements NotificationDeliveryRepository {
    JpaNotificationDeliveryRepository jpaNotificationDeliveryRepository;
    NotificationDeliveryPersistenceMapper notificationDeliveryPersistenceMapper;

    @Override
    public NotificationDelivery save(NotificationDelivery notificationDelivery) {
        return notificationDeliveryPersistenceMapper.toDomain(
                jpaNotificationDeliveryRepository.save(
                        notificationDeliveryPersistenceMapper.toEntity(notificationDelivery)
                )
        );
    }

    @Override
    @Transactional
    public void markReadByOutboxId(Long outboxId, LocalDateTime readAt) {
        jpaNotificationDeliveryRepository.markReadByOutboxId(outboxId, readAt);
    }

    @Override
    @Transactional
    public void markReadByRecipientUserId(Long userId, LocalDateTime readAt) {
        jpaNotificationDeliveryRepository.markReadByRecipientUserId(userId, readAt);
    }

    @Override
    @Transactional
    public void markReadByRecipientUserIdAndTarget(
            Long userId,
            String targetType,
            Long targetId,
            LocalDateTime readAt
    ) {
        jpaNotificationDeliveryRepository.markReadByRecipientUserIdAndTarget(userId, targetType, targetId, readAt);
    }
}
