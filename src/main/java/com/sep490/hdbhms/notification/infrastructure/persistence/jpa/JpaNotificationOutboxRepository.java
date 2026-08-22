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
                AND (:after <= 0 OR n.id < :after)
                AND n.status = 'SENT'
                AND n.sentAt IS NOT NULL
                AND n.scheduledAt <= n.sentAt
                ORDER BY n.id DESC
            """)
    List<NotificationOutboxEntity> findNextNotificationsCursor(
            @Param("userId") Long userId,
            @Param("channel") NotificationChannel channel,
            @Param("after") Long after,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM NotificationOutboxEntity n
            WHERE n.status = :status
              AND (
                    n.nextRetryAt <= :now
                    OR (n.nextRetryAt IS NULL AND n.scheduledAt <= :now)
              )
            ORDER BY COALESCE(n.nextRetryAt, n.scheduledAt), n.id
            """)
    List<NotificationOutboxEntity> findReadyPending(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM NotificationOutboxEntity n
            WHERE n.recipientUser.id = :userId
              AND n.channel = :channel
              AND n.status = 'SENT'
              AND n.sentAt IS NOT NULL
              AND n.scheduledAt <= n.sentAt
            ORDER BY n.createdAt DESC
            """)
    Page<NotificationOutboxEntity> findByRecipientUser_IdAndChannelOrderByCreatedAtDesc(
            Long userId,
            NotificationChannel channel,
            Pageable pageable
    );

    @Query(value = """
            SELECT n.* FROM notification_outbox n
            WHERE n.recipient_user_id = :userId
              AND n.channel = :channel
              AND (:after <= 0 OR n.notification_outbox_id < :after)
              AND n.status = 'SENT'
              AND n.sent_at IS NOT NULL
              AND n.scheduled_at <= n.sent_at
              AND (
                    JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.roomId')) = CAST(:roomId AS CHAR)
                    OR (
                        JSON_EXTRACT(n.payload, '$.roomId') IS NULL
                        AND
                        :roomCode IS NOT NULL
                        AND :roomCode <> ''
                        AND (
                            JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.roomCode')) = :roomCode
                            OR JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.room_code')) = :roomCode
                        )
                    )
              )
            ORDER BY n.notification_outbox_id DESC
            """, nativeQuery = true)
    List<NotificationOutboxEntity> findNextNotificationsCursorByRoomId(
            @Param("userId") Long userId,
            @Param("channel") String channel,
            @Param("after") Long after,
            @Param("roomId") Long roomId,
            @Param("roomCode") String roomCode,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(n) FROM NotificationOutboxEntity n
            WHERE n.recipientUser.id = :userId
              AND n.channel = :channel
              AND n.status = 'SENT'
              AND n.sentAt IS NOT NULL
              AND n.scheduledAt <= n.sentAt
              AND n.isRead = false
            """)
    long countByRecipientUser_IdAndChannelAndIsReadFalse(
            @Param("userId") Long userId,
            @Param("channel") NotificationChannel channel
    );

    @Query(value = """
            SELECT COUNT(*) FROM notification_outbox n
            WHERE n.recipient_user_id = :userId
              AND n.channel = :channel
              AND n.status = 'SENT'
              AND n.sent_at IS NOT NULL
              AND n.scheduled_at <= n.sent_at
              AND n.is_read = false
              AND (
                    JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.roomId')) = CAST(:roomId AS CHAR)
                    OR (
                        JSON_EXTRACT(n.payload, '$.roomId') IS NULL
                        AND
                        :roomCode IS NOT NULL
                        AND :roomCode <> ''
                        AND (
                            JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.roomCode')) = :roomCode
                            OR JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.room_code')) = :roomCode
                        )
                    )
              )
            """, nativeQuery = true)
    long countByRecipientUser_IdAndChannelAndIsReadFalseByRoom(
            @Param("userId") Long userId,
            @Param("channel") String channel,
            @Param("roomId") Long roomId,
            @Param("roomCode") String roomCode
    );

    @Modifying
    @Query("""
            UPDATE NotificationOutboxEntity n
            SET n.isRead = true
            WHERE n.recipientUser.id = :userId
              AND n.channel = :channel
              AND n.status = 'SENT'
              AND n.sentAt IS NOT NULL
              AND n.isRead = false
            """)
    void markAllAsRead(@Param("userId") Long userId, @Param("channel") NotificationChannel channel);

    @Modifying
    @Query("""
            UPDATE NotificationOutboxEntity n
            SET n.isRead = true,
                n.readAt = :readAt
            WHERE n.recipientUser.id = :userId
              AND n.channel = :channel
              AND n.status = 'SENT'
              AND n.sentAt IS NOT NULL
              AND n.isRead = false
            """)
    void markAllAsRead(
            @Param("userId") Long userId,
            @Param("channel") NotificationChannel channel,
            @Param("readAt") LocalDateTime readAt
    );

    @Modifying
    @Query(value = """
            UPDATE notification_outbox n
            SET n.is_read = true,
                n.read_at = :readAt
            WHERE n.recipient_user_id = :userId
              AND n.channel = :channel
              AND n.status = 'SENT'
              AND n.sent_at IS NOT NULL
              AND n.is_read = false
              AND (
                    JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.roomId')) = CAST(:roomId AS CHAR)
                    OR (
                        JSON_EXTRACT(n.payload, '$.roomId') IS NULL
                        AND
                        :roomCode IS NOT NULL
                        AND :roomCode <> ''
                        AND (
                            JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.roomCode')) = :roomCode
                            OR JSON_UNQUOTE(JSON_EXTRACT(n.payload, '$.room_code')) = :roomCode
                        )
                    )
              )
            """, nativeQuery = true)
    void markAllAsReadByRoomId(
            @Param("userId") Long userId,
            @Param("channel") String channel,
            @Param("roomId") Long roomId,
            @Param("roomCode") String roomCode,
            @Param("readAt") LocalDateTime readAt
    );

    @Modifying
    @Query("""
            UPDATE NotificationOutboxEntity n
            SET n.isRead = true,
                n.readAt = :readAt
            WHERE n.recipientUser.id = :userId
              AND n.status = 'SENT'
              AND n.sentAt IS NOT NULL
              AND n.isRead = false
            """)
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("""
            UPDATE NotificationOutboxEntity n
            SET n.isRead = true,
                n.readAt = :readAt
            WHERE n.recipientUser.id = :userId
              AND n.targetType = :targetType
              AND n.targetId = :targetId
              AND n.status = 'SENT'
              AND n.sentAt IS NOT NULL
              AND n.isRead = false
            """)
    void markTargetAsRead(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("readAt") LocalDateTime readAt
    );

    @Modifying
    @Query("UPDATE NotificationOutboxEntity n SET n.status = :newStatus WHERE n.id = :id AND n.status = 'PENDING'")
    int updateStatusIfPending(@Param("id") Long id, @Param("newStatus") OutboxStatus newStatus);
}
