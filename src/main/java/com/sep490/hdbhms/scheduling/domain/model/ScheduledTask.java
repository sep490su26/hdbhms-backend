package com.sep490.hdbhms.scheduling.domain.model;

import com.sep490.hdbhms.scheduling.domain.value_objects.TaskStatus;
import com.sep490.hdbhms.scheduling.domain.value_objects.TaskType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ScheduledTask {
    Long id;
    TaskType taskType;
    String targetType;
    Long targetId;
    LocalDateTime dueAt;
    TaskStatus status;
    @Setter
    Integer retryCount;
    byte[] payload;
    String idempotencyKey;
    Boolean recurring;
    String scheduleExpression;
    String lastError;
    LocalDateTime claimedAt;
    String claimedBy;
    LocalDateTime lockUntil;
    LocalDateTime executedAt;
    LocalDateTime createdAt;

    public static ScheduledTask create(TaskType taskType,
                                       String targetType,
                                       Long targetId,
                                       LocalDateTime dueAt) {
        return ScheduledTask.builder()
                .taskType(taskType)
                .targetType(targetType)
                .targetId(targetId)
                .dueAt(dueAt)
                .status(TaskStatus.PENDING)
                .retryCount(0)
                .idempotencyKey(defaultIdempotencyKey(taskType, targetType, targetId))
                .recurring(false)
                .build();
    }

    public void execute() {
        status = TaskStatus.DONE;
        executedAt = LocalDateTime.now();
        retryCount = 0;
        lastError = null;
        releaseClaim();
    }

    public void reschedule(LocalDateTime nextDueAt) {
        status = TaskStatus.PENDING;
        dueAt = nextDueAt;
        executedAt = LocalDateTime.now();
        retryCount = 0;
        lastError = null;
        releaseClaim();
    }

    public void retryLater(LocalDateTime nextDueAt, String errorMessage) {
        status = TaskStatus.PENDING;
        dueAt = nextDueAt;
        retryCount = retryCount == null ? 1 : retryCount + 1;
        lastError = errorMessage;
        releaseClaim();
    }

    public void failed(String errorMessage) {
        status = TaskStatus.FAILED;
        lastError = errorMessage;
        releaseClaim();
    }

    private void releaseClaim() {
        claimedAt = null;
        claimedBy = null;
        lockUntil = null;
    }

    private static String defaultIdempotencyKey(TaskType taskType, String targetType, Long targetId) {
        return taskType + ":" + targetType + ":" + targetId;
    }
}
