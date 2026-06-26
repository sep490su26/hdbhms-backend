package com.sep490.hdbhms.notification.infrastructure.persistence.entity;


import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.notification.domain.value_objects.NotificationChannel;
import com.sep490.hdbhms.notification.domain.value_objects.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "notification_outbox",
        indexes = {
                @Index(name = "idx_outbox_pending", columnList = "status, next_retry_at"),
                @Index(name = "idx_outbox_recipient", columnList = "recipient_user_id, created_at")
        }
)
public class NotificationOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_outbox_id")
    Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    String eventType;

    @Column(name = "target_type", length = 100)
    String targetType;

    @Column(name = "target_id")
    Long targetId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    UserEntity recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    NotificationChannel channel;

    @Column(nullable = false, length = 255)
    String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    String body;

    @Column(columnDefinition = "JSON")
    String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer maxRetries = 3;

    @Column(name = "last_error", length = 1000)
    String lastError;

    @Column(name = "is_read", nullable = false)
    Boolean isRead = false;

    @Column(name = "scheduled_at", nullable = false)
    LocalDateTime scheduledAt;

    @Column(name = "next_retry_at")
    LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    LocalDateTime sentAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}