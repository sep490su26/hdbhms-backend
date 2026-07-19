package com.sep490.hdbhms.scheduling.config;

import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskHandler;
import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskFailureClassifier;
import com.sep490.hdbhms.scheduling.application.handler.ScheduledTaskPolicy;
import com.sep490.hdbhms.scheduling.application.port.out.ScheduledTaskRepository;
import com.sep490.hdbhms.scheduling.domain.model.ScheduledTask;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ScheduledTaskProcessor {
    private static final int POLL_BATCH_SIZE = 50;
    private static final Duration MIN_HEARTBEAT_INTERVAL = Duration.ofSeconds(10);

    ScheduledTaskRepository scheduledTaskRepository;
    ScheduledTaskFailureClassifier scheduledTaskFailureClassifier;
    List<ScheduledTaskHandler> taskHandlers;
    ThreadPoolTaskExecutor scheduledTaskExecutor = createScheduledTaskExecutor();
    ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setName("scheduled-task-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    Map<TaskType, ScheduledTaskHandler> taskHandlerRegistry = new EnumMap<>(TaskType.class);
    Map<TaskType, Semaphore> taskTypeLocks = new ConcurrentHashMap<>();
    String workerId = createWorkerId();

    @PostConstruct
    public void registerTaskHandlers() {
        for (ScheduledTaskHandler taskHandler : taskHandlers) {
            for (TaskType taskType : taskHandler.supportedTaskTypes()) {
                ScheduledTaskHandler previous = taskHandlerRegistry.putIfAbsent(taskType, taskHandler);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate scheduled task handler for task type: " + taskType);
                }
            }
        }
    }

    @Scheduled(fixedDelay = 10_000)
    public void processDueTasks() {
        List<ScheduledTask> claimableTasks = scheduledTaskRepository.findClaimable(LocalDateTime.now(), POLL_BATCH_SIZE);
        for (ScheduledTask candidate : claimableTasks) {
            claimAndSubmit(candidate);
        }
    }

    private void claimAndSubmit(ScheduledTask candidate) {
        ScheduledTaskPolicy policy = policyFor(candidate.getTaskType());
        TaskExecutionLocks executionLocks = acquireExecutionLocks(candidate.getTaskType(), policy);
        if (executionLocks == null) {
            return;
        }

        LocalDateTime claimedAt = LocalDateTime.now();
        boolean claimed = scheduledTaskRepository.claim(
                candidate.getId(),
                claimedAt,
                claimedAt,
                claimedAt.plus(policy.lockDuration()),
                workerId
        );
        if (!claimed) {
            releaseExecutionLocks(executionLocks);
            return;
        }

        try {
            scheduledTaskExecutor.execute(() -> processClaimedTask(candidate.getId(), executionLocks, policy));
        } catch (RuntimeException e) {
            releaseExecutionLocks(executionLocks);
            log.error("Failed to submit scheduled task to worker pool. taskId={}, taskType={}: {}",
                    candidate.getId(),
                    candidate.getTaskType(),
                    e.getMessage(),
                    e);
        }
    }

    private void processClaimedTask(Long taskId, TaskExecutionLocks executionLocks, ScheduledTaskPolicy policy) {
        ScheduledFuture<?> heartbeat = startHeartbeat(taskId, executionLocks, policy);
        try {
            scheduledTaskRepository.findById(taskId)
                    .ifPresentOrElse(this::processClaimedTask, () -> log.warn(
                            "Claimed scheduled task disappeared before execution. taskId={}",
                            taskId
                    ));
        } finally {
            heartbeat.cancel(false);
            releaseExecutionLocks(executionLocks);
        }
    }

    private void processClaimedTask(ScheduledTask scheduledTask) {
        try {
            executeTask(scheduledTask);
            completeTask(scheduledTask);
        } catch (Exception e) {
            log.error("Scheduled task failed. taskId={}, taskType={}: {}",
                    scheduledTask.getId(),
                    scheduledTask.getTaskType(),
                    e.getMessage(),
                    e);
            failTask(scheduledTask, e);
        }

        boolean saved = scheduledTaskRepository.saveClaimedResult(scheduledTask, workerId);
        if (!saved) {
            log.warn("Skipped saving scheduled task result because claim moved. taskId={}, taskType={}, workerId={}",
                    scheduledTask.getId(),
                    scheduledTask.getTaskType(),
                    workerId);
        }
    }

    private TaskExecutionLocks acquireExecutionLocks(TaskType taskType, ScheduledTaskPolicy policy) {
        if (!policy.singleInstance()) {
            return TaskExecutionLocks.none();
        }

        Semaphore localLock = taskTypeLocks.computeIfAbsent(taskType, ignored -> new Semaphore(1));
        if (!localLock.tryAcquire()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean acquiredGlobalLock = scheduledTaskRepository.acquireTaskTypeLock(
                taskType,
                now,
                now.plus(policy.lockDuration()),
                workerId
        );
        if (!acquiredGlobalLock) {
            localLock.release();
            return null;
        }

        return new TaskExecutionLocks(taskType, localLock, true);
    }

    private void releaseExecutionLocks(TaskExecutionLocks executionLocks) {
        if (executionLocks.globalLockHeld()) {
            scheduledTaskRepository.releaseTaskTypeLock(executionLocks.taskType(), workerId);
        }
        if (executionLocks.localLock() != null) {
            executionLocks.localLock().release();
        }
    }

    private void completeTask(ScheduledTask scheduledTask) {
        if (Boolean.TRUE.equals(scheduledTask.getRecurring())) {
            scheduledTask.reschedule(
                    RecurringSystemJobSchedule.nextDueAt(scheduledTask.getTaskType(), LocalDateTime.now())
            );
            return;
        }
        scheduledTask.execute();
    }

    private void failTask(ScheduledTask scheduledTask, Exception exception) {
        ScheduledTaskPolicy policy = policyFor(scheduledTask.getTaskType());
        String errorMessage = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        boolean retryable = scheduledTaskFailureClassifier.retryable(exception);
        if (!retryable) {
            log.warn("Scheduled task marked as non-retryable. taskId={}, taskType={}, error={}",
                    scheduledTask.getId(),
                    scheduledTask.getTaskType(),
                    errorMessage);
        }

        if (Boolean.TRUE.equals(scheduledTask.getRecurring())) {
            if (!retryable) {
                scheduledTask.retryLater(
                        RecurringSystemJobSchedule.nextDueAt(scheduledTask.getTaskType(), LocalDateTime.now()),
                        errorMessage
                );
                return;
            }
            int retryCount = scheduledTask.getRetryCount() == null ? 0 : scheduledTask.getRetryCount();
            LocalDateTime nextRun = retryCount >= policy.maxAttempts() - 1
                    ? RecurringSystemJobSchedule.nextDueAt(scheduledTask.getTaskType(), LocalDateTime.now())
                    : LocalDateTime.now().plus(policy.retryDelay());
            scheduledTask.retryLater(nextRun, errorMessage);
            return;
        }

        int retryCount = scheduledTask.getRetryCount() == null ? 0 : scheduledTask.getRetryCount();
        if (!retryable || retryCount >= policy.maxAttempts() - 1) {
            scheduledTask.setRetryCount(retryCount + 1);
            scheduledTask.failed(errorMessage);
            return;
        }

        scheduledTask.retryLater(LocalDateTime.now().plus(policy.retryDelay()), errorMessage);
    }

    private void executeTask(ScheduledTask scheduledTask) {
        ScheduledTaskHandler taskHandler = taskHandlerRegistry.get(scheduledTask.getTaskType());
        if (taskHandler == null) {
            throw new IllegalStateException("No scheduled task handler registered for task type: " + scheduledTask.getTaskType());
        }
        taskHandler.handle(scheduledTask);
    }

    private ScheduledTaskPolicy policyFor(TaskType taskType) {
        ScheduledTaskHandler taskHandler = taskHandlerRegistry.get(taskType);
        if (taskHandler == null) {
            return ScheduledTaskPolicy.standard();
        }
        return taskHandler.policy(taskType);
    }

    private ScheduledFuture<?> startHeartbeat(Long taskId, TaskExecutionLocks executionLocks, ScheduledTaskPolicy policy) {
        Duration interval = heartbeatInterval(policy.lockDuration());
        return heartbeatExecutor.scheduleAtFixedRate(
                () -> extendLocks(taskId, executionLocks, policy),
                interval.toMillis(),
                interval.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    private void extendLocks(Long taskId, TaskExecutionLocks executionLocks, ScheduledTaskPolicy policy) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime lockUntil = now.plus(policy.lockDuration());
            boolean taskLockExtended = scheduledTaskRepository.extendClaimLock(taskId, lockUntil, workerId);
            if (!taskLockExtended) {
                log.warn("Failed to extend scheduled task lock. taskId={}, workerId={}", taskId, workerId);
                return;
            }
            if (executionLocks.globalLockHeld()) {
                boolean typeLockExtended = scheduledTaskRepository.extendTaskTypeLock(
                        executionLocks.taskType(),
                        now,
                        lockUntil,
                        workerId
                );
                if (!typeLockExtended) {
                    log.warn("Failed to extend scheduled task type lock. taskId={}, taskType={}, workerId={}",
                            taskId,
                            executionLocks.taskType(),
                            workerId);
                }
            }
        } catch (RuntimeException exception) {
            log.warn("Scheduled task heartbeat failed. taskId={}, workerId={}: {}",
                    taskId,
                    workerId,
                    exception.getMessage(),
                    exception);
        }
    }

    private Duration heartbeatInterval(Duration lockDuration) {
        Duration interval = lockDuration.dividedBy(3);
        return interval.compareTo(MIN_HEARTBEAT_INTERVAL) < 0 ? MIN_HEARTBEAT_INTERVAL : interval;
    }

    private static String createWorkerId() {
        String hostName = System.getenv("COMPUTERNAME");
        if (hostName == null || hostName.isBlank()) {
            hostName = System.getenv("HOSTNAME");
        }
        if (hostName == null || hostName.isBlank()) {
            hostName = "worker";
        }
        return hostName + "-" + UUID.randomUUID();
    }

    private static ThreadPoolTaskExecutor createScheduledTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("scheduled-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @PreDestroy
    public void shutdownExecutor() {
        heartbeatExecutor.shutdownNow();
        scheduledTaskExecutor.shutdown();
    }

    private record TaskExecutionLocks(TaskType taskType, Semaphore localLock, boolean globalLockHeld) {
        private static TaskExecutionLocks none() {
            return new TaskExecutionLocks(null, null, false);
        }
    }
}
