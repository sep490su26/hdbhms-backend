package com.sep490.hdbhms.notification.infrastructure.persistence.repository;

import com.sep490.hdbhms.notification.application.port.out.NotificationOutboxRepository;
import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.persistence.jpa.JpaNotificationOutboxRepository;
import com.sep490.hdbhms.notification.infrastructure.persistence.mapper.NotificationOutboxPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataNotificationOutboxRepository implements NotificationOutboxRepository {
    JpaNotificationOutboxRepository jpaNotificationOutboxRepository;
    NotificationOutboxPersistenceMapper notificationOutboxPersistenceMapper;

    @Override
    public NotificationOutbox save(NotificationOutbox notificationOutbox) {
        return notificationOutboxPersistenceMapper.toDomain(
                jpaNotificationOutboxRepository.save(
                        notificationOutboxPersistenceMapper.toEntity(
                                notificationOutbox
                        )
                )
        );
    }

    @Override
    public Optional<NotificationOutbox> findById(Long id) {
        return jpaNotificationOutboxRepository.findById(id)
                .map(notificationOutboxPersistenceMapper::toDomain);
    }

    @Override
    public List<NotificationOutbox> findByStatusAndNextRetryAtBefore(OutboxStatus outboxStatus, LocalDateTime localDateTime) {
        return jpaNotificationOutboxRepository
                .findByStatusAndNextRetryAtBefore(outboxStatus, localDateTime).stream()
                .map(notificationOutboxPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public org.springframework.data.domain.Page<NotificationOutbox> findByRecipientUserIdAndChannelOrderByCreatedAtDesc(Long userId, com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel channel, org.springframework.data.domain.Pageable pageable) {
        return jpaNotificationOutboxRepository.findByRecipientUser_IdAndChannelOrderByCreatedAtDesc(userId, channel, pageable)
                .map(notificationOutboxPersistenceMapper::toDomain);
    }

    @Override
    public List<NotificationOutbox> findNextNotificationsCursor(Long userId, com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel channel, long after, int limit) {
        return jpaNotificationOutboxRepository.findNextNotificationsCursor(userId, channel, after, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(notificationOutboxPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public long countByRecipientUserIdAndIsReadFalse(Long userId) {
        return jpaNotificationOutboxRepository.countByRecipientUser_IdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId, LocalDateTime readAt) {
        jpaNotificationOutboxRepository.markAllAsRead(userId, readAt);
    }

    @Override
    @Transactional
    public void markTargetAsRead(Long userId, String targetType, Long targetId, LocalDateTime readAt) {
        jpaNotificationOutboxRepository.markTargetAsRead(userId, targetType, targetId, readAt);
    }

    @Override
    @Transactional
    public boolean markAsProcessing(Long id) {
        int updated = jpaNotificationOutboxRepository.updateStatusIfPending(id, OutboxStatus.PROCESSING);
        return updated > 0;
    }
}
