package com.sep490.hdbhms.notification.infrastructure.persistence.jpa;

import com.sep490.hdbhms.notification.domain.model.NotificationOutbox;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationOutboxEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.Modifying;

public interface JpaNotificationOutboxRepository extends JpaRepository<NotificationOutboxEntity, Long> {

    @Query("""
            SELECT n from NotificationOutboxEntity n
                WHERE n.recipientUser.id = :userId
                AND n.channel = :channel
                AND n.id > :after
                            ORDER BY n.id DESC
            """)
    List<NotificationOutboxEntity> findNextNotificationsCursor(
            @Param("userId") Long userId,
            @Param("channel") NotificationChannel channel,
            @Param("after") Long after,
            Pageable pageable
    );

    List<NotificationOutboxEntity> findByStatusAndNextRetryAtBefore(OutboxStatus outboxStatus, LocalDateTime localDateTime);

    Page<NotificationOutboxEntity> findByRecipientUser_IdAndChannelOrderByCreatedAtDesc(Long userId, NotificationChannel channel, Pageable pageable);

    long countByRecipientUser_IdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE NotificationOutboxEntity n SET n.isRead = true WHERE n.recipientUser.id = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE NotificationOutboxEntity n SET n.status = :newStatus WHERE n.id = :id AND n.status = 'PENDING'")
    int updateStatusIfPending(@Param("id") Long id, @Param("newStatus") OutboxStatus newStatus);
}
