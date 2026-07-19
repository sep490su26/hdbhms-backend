package com.sep490.hdbhms.scheduling.infrastructure.persistence.repository;

import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskStatus;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.entity.ScheduledTaskEntity;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.jpa.JpaScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.infrastructure.persistence.mapper.ScheduledTaskPersistenceMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SpringDataScheduledTaskRepository implements ScheduledTaskRepository {
    JpaScheduledTaskRepository jpaScheduledTaskRepository;
    ScheduledTaskPersistenceMapper scheduledTaskPersistenceMapper;

    @Override
    public ScheduledTask save(ScheduledTask scheduledTask) {
        return scheduledTaskPersistenceMapper.toDomain(
                jpaScheduledTaskRepository.save(
                        scheduledTaskPersistenceMapper.toEntity(
                                scheduledTask
                        )
                )
        );
    }

    @Override
    public void ensureOneOffTask(TaskType taskType, String targetType, Long targetId, LocalDateTime dueAt, String idempotencyKey) {
        jpaScheduledTaskRepository.ensureOneOffTask(
                taskType.name(),
                targetType,
                targetId,
                dueAt,
                idempotencyKey
        );
    }

    @Override
    public void ensureRecurringSystemTask(TaskType taskType, LocalDateTime dueAt, String scheduleExpression, String idempotencyKey) {
        jpaScheduledTaskRepository.ensureRecurringSystemTask(
                taskType.name(),
                dueAt,
                scheduleExpression,
                idempotencyKey
        );
    }

    @Override
    public Optional<ScheduledTask> findById(Long id) {
        return jpaScheduledTaskRepository.findById(id)
                .map(scheduledTaskPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ScheduledTask> findByTaskTypeAndTargetTypeAndTargetId(TaskType taskType, String targetType, Long targetId) {
        return jpaScheduledTaskRepository.findByTaskTypeAndTargetTypeAndTargetId(taskType, targetType, targetId)
                .map(scheduledTaskPersistenceMapper::toDomain);
    }

    @Override
    public List<ScheduledTask> findClaimable(LocalDateTime now, int limit) {
        return jpaScheduledTaskRepository
                .findClaimable(TaskStatus.PENDING, TaskStatus.PROCESSING, now, PageRequest.of(0, limit)).stream()
                .map(scheduledTaskPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean claim(Long id, LocalDateTime now, LocalDateTime claimedAt, LocalDateTime lockUntil, String workerId) {
        return jpaScheduledTaskRepository.claim(
                id,
                TaskStatus.PENDING,
                TaskStatus.PROCESSING,
                now,
                claimedAt,
                lockUntil,
                workerId
        ) == 1;
    }

    @Override
    public boolean saveClaimedResult(ScheduledTask scheduledTask, String workerId) {
        return jpaScheduledTaskRepository.saveClaimedResult(
                scheduledTask.getId(),
                scheduledTask.getStatus(),
                scheduledTask.getDueAt(),
                scheduledTask.getRetryCount(),
                scheduledTask.getLastError(),
                scheduledTask.getExecutedAt(),
                TaskStatus.PROCESSING,
                workerId
        ) == 1;
    }

    @Override
    public boolean extendClaimLock(Long id, LocalDateTime lockUntil, String workerId) {
        return jpaScheduledTaskRepository.extendClaimLock(
                id,
                TaskStatus.PROCESSING,
                lockUntil,
                workerId
        ) == 1;
    }

    @Override
    public boolean acquireTaskTypeLock(TaskType taskType, LocalDateTime now, LocalDateTime lockUntil, String workerId) {
        jpaScheduledTaskRepository.acquireTaskTypeLock(taskType.name(), now, lockUntil, workerId);
        return jpaScheduledTaskRepository.countTaskTypeLockHeldBy(taskType.name(), workerId, now) == 1;
    }

    @Override
    public boolean extendTaskTypeLock(TaskType taskType, LocalDateTime now, LocalDateTime lockUntil, String workerId) {
        return jpaScheduledTaskRepository.extendTaskTypeLock(taskType.name(), now, lockUntil, workerId) == 1;
    }

    @Override
    public void releaseTaskTypeLock(TaskType taskType, String workerId) {
        jpaScheduledTaskRepository.releaseTaskTypeLock(taskType.name(), workerId);
    }

    @Override
    public boolean retryFailed(Long id, LocalDateTime dueAt) {
        return jpaScheduledTaskRepository.retryFailed(
                id,
                dueAt,
                TaskStatus.PENDING,
                TaskStatus.FAILED
        ) == 1;
    }

    @Override
    public boolean cancelFailed(Long id) {
        return jpaScheduledTaskRepository.cancelFailed(
                id,
                TaskStatus.CANCELLED,
                TaskStatus.FAILED
        ) == 1;
    }

    @Override
    public void cancelForTarget(String targetType, Long targetId) {
        jpaScheduledTaskRepository.changeScheduleTaskStatus(
                targetType,
                targetId,
                TaskStatus.PENDING,
                TaskStatus.CANCELLED
        );
    }
}
