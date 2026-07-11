package com.sep490.hdbhms.notification.infrastructure.persistence.jpa;

import com.sep490.hdbhms.notification.infrastructure.persistence.entity.NotificationDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface JpaNotificationDeliveryRepository extends JpaRepository<NotificationDeliveryEntity, Long> {
    @Modifying
    @Query("""
            UPDATE NotificationDeliveryEntity d
            SET d.readAt = :readAt
            WHERE d.outbox.id = :outboxId
              AND d.readAt IS NULL
            """)
    void markReadByOutboxId(@Param("outboxId") Long outboxId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("""
            UPDATE NotificationDeliveryEntity d
            SET d.readAt = :readAt
            WHERE d.outbox.recipientUser.id = :userId
              AND d.readAt IS NULL
            """)
    void markReadByRecipientUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Modifying
    @Query("""
            UPDATE NotificationDeliveryEntity d
            SET d.readAt = :readAt
            WHERE d.outbox.recipientUser.id = :userId
              AND d.outbox.targetType = :targetType
              AND d.outbox.targetId = :targetId
              AND d.readAt IS NULL
            """)
    void markReadByRecipientUserIdAndTarget(
            @Param("userId") Long userId,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("readAt") LocalDateTime readAt
    );
}
