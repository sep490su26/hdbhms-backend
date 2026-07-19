package com.sep490.hdbhms.occupancy.infrastructure.persistence.entity;

import com.sep490.hdbhms.billingandpayment.infrastructure.persistence.entity.ManagerTaskEntity;
import com.sep490.hdbhms.identityandaccess.infrastructure.persistence.entity.UserEntity;
import com.sep490.hdbhms.occupancy.domain.value_objects.ReminderTrackerStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "reminder_trackers",
        indexes = {
                @Index(name = "idx_reminder_tracker_due", columnList = "status, next_due_at"),
                @Index(name = "idx_reminder_tracker_target", columnList = "reminder_key, target_type, target_id, audience")
        }
)
public class ReminderTrackerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reminder_tracker_id")
    Long id;

    @Column(name = "reminder_key", nullable = false, length = 100)
    String reminderKey;

    @Column(name = "target_type", nullable = false, length = 50)
    String targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @Column(nullable = false, length = 50)
    String audience;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id")
    UserEntity recipientUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    ReminderTrackerStatus status = ReminderTrackerStatus.ACTIVE;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    Integer sentCount = 0;

    @Column(name = "last_sent_at")
    LocalDateTime lastSentAt;

    @Column(name = "next_due_at")
    LocalDateTime nextDueAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_task_id")
    ManagerTaskEntity relatedTask;

    @Column(columnDefinition = "JSON")
    String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
