package com.sep490.hdbhms.scheduling.infrastructure.persistence.entity;

import com.sep490.hdbhms.scheduling.domain.value_objects.TaskStatus;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
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
        name = "scheduled_tasks",
        indexes = {
                @Index(name = "idx_tasks_due", columnList = "status, due_at"),
                @Index(name = "idx_tasks_claimable", columnList = "status, due_at, lock_until")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_scheduled_tasks_idempotency_key", columnNames = "idempotency_key")
        }
)
public class ScheduledTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "scheduled_task_id")
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 50)
    TaskType taskType;

    @Column(name = "target_type", nullable = false, length = 100)
    String targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    @Column(name = "due_at", nullable = false)
    LocalDateTime dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    TaskStatus status = TaskStatus.PENDING;

    @Column(name = "retry_count", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    @Builder.Default
    Integer retryCount = 0;

    @Lob
    @Column(columnDefinition = "BLOB")
    byte[] payload;

    @Column(name = "idempotency_key", length = 180)
    String idempotencyKey;

    @Column(nullable = false)
    @Builder.Default
    Boolean recurring = false;

    @Column(name = "schedule_expression", length = 100)
    String scheduleExpression;

    @Column(name = "last_error", columnDefinition = "TEXT")
    String lastError;

    @Column(name = "claimed_at")
    LocalDateTime claimedAt;

    @Column(name = "claimed_by", length = 120)
    String claimedBy;

    @Column(name = "lock_until")
    LocalDateTime lockUntil;

    @Column(name = "executed_at")
    LocalDateTime executedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    LocalDateTime createdAt;
}
