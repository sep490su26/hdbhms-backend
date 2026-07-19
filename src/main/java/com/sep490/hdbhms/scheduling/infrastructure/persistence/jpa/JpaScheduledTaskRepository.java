package com.sep490.hdbhms.scheduling.infrastructure.persistence.jpa;

import com.sep490.hdbhms.scheduling.domain.value_objects.TaskStatus;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.entity.ScheduledTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaScheduledTaskRepository extends JpaRepository<ScheduledTaskEntity, Long> {
    java.util.Optional<ScheduledTaskEntity> findByTaskTypeAndTargetTypeAndTargetId(
            TaskType taskType,
            String targetType,
            Long targetId
    );

    @Transactional
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO scheduled_tasks
                (task_type, target_type, target_id, due_at, status, retry_count, idempotency_key, recurring, schedule_expression)
            VALUES
                (:taskType, :targetType, :targetId, :dueAt, 'PENDING', 0, :idempotencyKey, 0, null)
            """, nativeQuery = true)
    int ensureOneOffTask(
            @Param("taskType") String taskType,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("dueAt") LocalDateTime dueAt,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Transactional
    @Modifying
    @Query(value = """
            INSERT IGNORE INTO scheduled_tasks
                (task_type, target_type, target_id, due_at, status, retry_count, idempotency_key, recurring, schedule_expression)
            VALUES
                (:taskType, 'SYSTEM_JOB', 0, :dueAt, 'PENDING', 0, :idempotencyKey, 1, :scheduleExpression)
            """, nativeQuery = true)
    int ensureRecurringSystemTask(
            @Param("taskType") String taskType,
            @Param("dueAt") LocalDateTime dueAt,
            @Param("scheduleExpression") String scheduleExpression,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Query("""
            SELECT t FROM ScheduledTaskEntity t
            WHERE (t.status = :pendingStatus AND t.dueAt <= :now)
               OR (t.status = :processingStatus AND t.lockUntil <= :now)
            ORDER BY t.dueAt ASC
            """)
    List<ScheduledTaskEntity> findClaimable(
            @Param("pendingStatus") TaskStatus pendingStatus,
            @Param("processingStatus") TaskStatus processingStatus,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledTaskEntity t
            SET t.status = :processingStatus,
                t.claimedAt = :claimedAt,
                t.claimedBy = :workerId,
                t.lockUntil = :lockUntil
            WHERE t.id = :id
              AND (
                  (t.status = :pendingStatus AND t.dueAt <= :now)
                  OR (t.status = :processingStatus AND t.lockUntil <= :now)
              )
            """)
    int claim(
            @Param("id") Long id,
            @Param("pendingStatus") TaskStatus pendingStatus,
            @Param("processingStatus") TaskStatus processingStatus,
            @Param("now") LocalDateTime now,
            @Param("claimedAt") LocalDateTime claimedAt,
            @Param("lockUntil") LocalDateTime lockUntil,
            @Param("workerId") String workerId
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledTaskEntity t
            SET t.status = :status,
                t.dueAt = :dueAt,
                t.retryCount = :retryCount,
                t.lastError = :lastError,
                t.executedAt = :executedAt,
                t.claimedAt = null,
                t.claimedBy = null,
                t.lockUntil = null
            WHERE t.id = :id
              AND t.status = :processingStatus
              AND t.claimedBy = :workerId
            """)
    int saveClaimedResult(
            @Param("id") Long id,
            @Param("status") TaskStatus status,
            @Param("dueAt") LocalDateTime dueAt,
            @Param("retryCount") Integer retryCount,
            @Param("lastError") String lastError,
            @Param("executedAt") LocalDateTime executedAt,
            @Param("processingStatus") TaskStatus processingStatus,
            @Param("workerId") String workerId
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledTaskEntity t
            SET t.lockUntil = :lockUntil
            WHERE t.id = :id
              AND t.status = :processingStatus
              AND t.claimedBy = :workerId
            """)
    int extendClaimLock(
            @Param("id") Long id,
            @Param("processingStatus") TaskStatus processingStatus,
            @Param("lockUntil") LocalDateTime lockUntil,
            @Param("workerId") String workerId
    );

    @Transactional
    @Modifying
    @Query(value = """
            INSERT INTO scheduled_task_type_locks
                (task_type, claimed_by, lock_until, updated_at)
            VALUES
                (:taskType, :workerId, :lockUntil, :now)
            ON DUPLICATE KEY UPDATE
                claimed_by = IF(lock_until <= VALUES(updated_at) OR claimed_by = VALUES(claimed_by), VALUES(claimed_by), claimed_by),
                lock_until = IF(lock_until <= VALUES(updated_at) OR claimed_by = VALUES(claimed_by), VALUES(lock_until), lock_until),
                updated_at = IF(lock_until <= VALUES(updated_at) OR claimed_by = VALUES(claimed_by), VALUES(updated_at), updated_at)
            """, nativeQuery = true)
    int acquireTaskTypeLock(
            @Param("taskType") String taskType,
            @Param("now") LocalDateTime now,
            @Param("lockUntil") LocalDateTime lockUntil,
            @Param("workerId") String workerId
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM scheduled_task_type_locks
            WHERE task_type = :taskType
              AND claimed_by = :workerId
              AND lock_until > :now
            """, nativeQuery = true)
    long countTaskTypeLockHeldBy(
            @Param("taskType") String taskType,
            @Param("workerId") String workerId,
            @Param("now") LocalDateTime now
    );

    @Transactional
    @Modifying
    @Query(value = """
            UPDATE scheduled_task_type_locks
            SET lock_until = :lockUntil,
                updated_at = :now
            WHERE task_type = :taskType
              AND claimed_by = :workerId
            """, nativeQuery = true)
    int extendTaskTypeLock(
            @Param("taskType") String taskType,
            @Param("now") LocalDateTime now,
            @Param("lockUntil") LocalDateTime lockUntil,
            @Param("workerId") String workerId
    );

    @Transactional
    @Modifying
    @Query(value = """
            DELETE FROM scheduled_task_type_locks
            WHERE task_type = :taskType
              AND claimed_by = :workerId
            """, nativeQuery = true)
    void releaseTaskTypeLock(
            @Param("taskType") String taskType,
            @Param("workerId") String workerId
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledTaskEntity t
            SET t.status = :pendingStatus,
                t.dueAt = :dueAt,
                t.retryCount = 0,
                t.lastError = null,
                t.claimedAt = null,
                t.claimedBy = null,
                t.lockUntil = null
            WHERE t.id = :id
              AND t.status = :failedStatus
            """)
    int retryFailed(
            @Param("id") Long id,
            @Param("dueAt") LocalDateTime dueAt,
            @Param("pendingStatus") TaskStatus pendingStatus,
            @Param("failedStatus") TaskStatus failedStatus
    );

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ScheduledTaskEntity t
            SET t.status = :cancelledStatus,
                t.claimedAt = null,
                t.claimedBy = null,
                t.lockUntil = null
            WHERE t.id = :id
              AND t.status = :failedStatus
            """)
    int cancelFailed(
            @Param("id") Long id,
            @Param("cancelledStatus") TaskStatus cancelledStatus,
            @Param("failedStatus") TaskStatus failedStatus
    );

    @Transactional
    @Modifying
    @Query("UPDATE ScheduledTaskEntity t SET t.status = :newStatus WHERE t.targetType = :type AND t.targetId = :id AND t.status = :oldStatus")
    void changeScheduleTaskStatus(@Param("type") String type, @Param("id") Long id,
                                  @Param("oldStatus") TaskStatus old, @Param("newStatus") TaskStatus newStatus);
}
