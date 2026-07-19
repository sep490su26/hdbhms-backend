package com.sep490.hdbhms.scheduling.application.port.out;

import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduledTaskRepository {
    ScheduledTask save(ScheduledTask scheduledTask);

    void ensureOneOffTask(TaskType taskType, String targetType, Long targetId, LocalDateTime dueAt, String idempotencyKey);

    void ensureRecurringSystemTask(TaskType taskType, LocalDateTime dueAt, String scheduleExpression, String idempotencyKey);

    Optional<ScheduledTask> findById(Long id);

    Optional<ScheduledTask> findByTaskTypeAndTargetTypeAndTargetId(TaskType taskType, String targetType, Long targetId);

    List<ScheduledTask> findClaimable(LocalDateTime now, int limit);

    boolean claim(Long id, LocalDateTime now, LocalDateTime claimedAt, LocalDateTime lockUntil, String workerId);

    boolean saveClaimedResult(ScheduledTask scheduledTask, String workerId);

    boolean extendClaimLock(Long id, LocalDateTime lockUntil, String workerId);

    boolean acquireTaskTypeLock(TaskType taskType, LocalDateTime now, LocalDateTime lockUntil, String workerId);

    boolean extendTaskTypeLock(TaskType taskType, LocalDateTime now, LocalDateTime lockUntil, String workerId);

    void releaseTaskTypeLock(TaskType taskType, String workerId);

    boolean retryFailed(Long id, LocalDateTime dueAt);

    boolean cancelFailed(Long id);

    void cancelForTarget(String targetType, Long targetId);
}
